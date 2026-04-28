package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.commission.CommissionSettings;
import com.blueWhale.Rahwan.commission.CommissionSettingsService;
import com.blueWhale.Rahwan.order.service.CostCalculationService;
import com.blueWhale.Rahwan.order.service.PricingDetails;
import com.blueWhale.Rahwan.otp.OrderOtpService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import com.blueWhale.Rahwan.util.ImageUtility;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletService;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CostCalculationService costCalculationService;
    @Mock
    private WalletService walletService;
    @Mock
    private OrderOtpService otpService;
    @Mock
    private WhatsAppService whatsAppService;
    @Mock
    private CommissionSettingsService commissionSettingsService;

    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderService = new OrderService(orderRepository, orderMapper, userRepository, costCalculationService,
                walletService, otpService, whatsAppService, commissionSettingsService);
        // make save return the same entity by default to avoid null returns in service methods
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void createOrder_withoutPhoto_savesOrderAndReturnsCreationDto() throws IOException {
        UUID userId = UUID.randomUUID();

        // mock user
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.isUser()).thenReturn(true);
        when(user.isDriver()).thenReturn(true);

        // mock OrderForm with simple numeric values and no photo
        OrderForm form = mock(OrderForm.class);
        when(form.getPickupLatitude()).thenReturn(0.0);
        when(form.getPickupLongitude()).thenReturn(0.0);
        when(form.getRecipientLatitude()).thenReturn(0.0);
        when(form.getRecipientLongitude()).thenReturn(0.0);
        when(form.getInsuranceValue()).thenReturn(0.0);
        when(form.getPhoto()).thenReturn(null);

        // mock pricing and commission
        PricingDetails pricing = mock(PricingDetails.class);
        when(pricing.getTotalCost()).thenReturn(50.0);
        when(pricing.getDistanceKm()).thenReturn(10.0);
        when(costCalculationService.calculateCost(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(pricing);

        CommissionSettings cs = mock(CommissionSettings.class);
        when(cs.getCommissionRate()).thenReturn(5.0);
        when(commissionSettingsService.getActiveSettings()).thenReturn(cs);

        // mapper and repository behavior
        Order mappedOrder = new Order();
        when(orderMapper.toEntity(form)).thenReturn(mappedOrder);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreationDto creationDto = new CreationDto();
        creationDto.setUserId(userId);
        lenient().doReturn(creationDto).when(orderMapper).toCreationDto(any(Order.class));

        CreationDto result = orderService.createOrder(form, userId);

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(costCalculationService, times(1)).calculateCost(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    public void confirmOrder_freezesWalletAndSendsOtp() {
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.isUser()).thenReturn(true);
        when(user.getPhone()).thenReturn("+201234567890");
        when(user.getName()).thenReturn("Test User");

        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setCreationStatus(CreationStatus.CREATED);
        order.setDeliveryCost(100.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Wallet wallet = new Wallet();
        wallet.setWalletBalance(200.0);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        when(otpService.generatePickupOtp()).thenReturn("0000");

        OrderDto dto = new OrderDto();
        dto.setUserId(userId);
        lenient().doReturn(dto).when(orderMapper).toDto(any(Order.class));

        OrderDto result = orderService.confirmOrder(1L, userId);

        assertNotNull(result);
        verify(walletService, times(1)).freezeAmount(eq(wallet), eq(200.0)); // deliveryCost * 2
        verify(whatsAppService, times(1)).sendPickupOtpToSender(eq("+201234567890"), eq("Test User"), eq("0000"));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    public void updateOrder_withPending_unfreezesAndFreezesWalletAndSaves() throws IOException {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.isUser()).thenReturn(true);

        Order order = new Order();
        order.setId(2L);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryCost(100.0);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        OrderForm form = mock(OrderForm.class);
        when(form.getPickupLatitude()).thenReturn(1.0);
        when(form.getPickupLongitude()).thenReturn(1.0);
        when(form.getRecipientLatitude()).thenReturn(2.0);
        when(form.getRecipientLongitude()).thenReturn(2.0);
        when(form.getInsuranceValue()).thenReturn(10.0);
        when(form.getPhoto()).thenReturn(null);

        PricingDetails pricing = mock(PricingDetails.class);
        when(pricing.getTotalCost()).thenReturn(120.0);
        when(pricing.getDistanceKm()).thenReturn(5.0);
        when(costCalculationService.calculateCost(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(pricing);

        CommissionSettings cs = mock(CommissionSettings.class);
        when(cs.getCommissionRate()).thenReturn(2.0);
        when(commissionSettingsService.getActiveSettings()).thenReturn(cs);

        Wallet userWallet = new Wallet();
        when(walletService.getWalletByUserId(userId)).thenReturn(userWallet);

        lenient().doReturn(new CreationDto()).when(orderMapper).toCreationDto(any(Order.class));

        CreationDto dto = orderService.updateOrder(2L, form, userId);

        assertNotNull(dto);
        verify(walletService, times(1)).unfreezeAmount(eq(userWallet), eq(200.0));
        verify(walletService, times(1)).freezeAmount(eq(userWallet), eq(240.0)); // totalCost *2
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    public void driverConfirmOrder_acceptsAndSendsNotification() {
        UUID driverId = UUID.randomUUID();
        User driver = mock(User.class);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driver.isActive()).thenReturn(true);
        when(driver.isDriver()).thenReturn(true);

        Order order = new Order();
        order.setId(3L);
        order.setStatus(OrderStatus.PENDING);
        order.setCreationStatus(CreationStatus.CONFIRMED);
        order.setUserId(UUID.randomUUID());
        order.setInsuranceValue(50.0);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        Wallet driverWallet = new Wallet();
        driverWallet.setWalletBalance(100.0);
        when(walletService.getWalletByUserId(driverId)).thenReturn(driverWallet);

        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));

        OrderDto res = orderService.driverConfirmOrder(3L, driverId);

        assertNotNull(res);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(whatsAppService, atMost(1)).sendDriverAcceptedNotification(anyString(), anyString(), anyString());
    }

    @Test
    public void confirmPickup_freezesDriverAndSendsDeliveryOtp() {
        UUID driverId = UUID.randomUUID();
        User driver = mock(User.class);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driver.isActive()).thenReturn(true);
        when(driver.isDriver()).thenReturn(true);

        Order order = new Order();
        order.setId(4L);
        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setOtpForPickup("1234");
        order.setInsuranceValue(30.0);
        order.setRecipientPhone("+200000");
        order.setRecipientName("R");
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        Wallet driverWallet = new Wallet();
        when(walletService.getWalletByUserId(driverId)).thenReturn(driverWallet);

        when(otpService.generateDeliveryOtp()).thenReturn("5678");
        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));

        OrderDto res = orderService.confirmPickup(4L, driverId, "1234");

        assertNotNull(res);
        verify(walletService, times(1)).freezeAmount(eq(driverWallet), eq(30.0));
        verify(whatsAppService, times(1)).sendDeliveryOtpToRecipient(eq(order.getRecipientPhone()), eq(order.getRecipientName()), eq("5678"));
    }

    @Test
    public void confirmDelivery_unfreezesAndTransfersFunds() {
        UUID driverId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User driver = mock(User.class);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driver.isActive()).thenReturn(true);
        when(driver.isDriver()).thenReturn(true);

        Order order = new Order();
        order.setId(5L);
        order.setDriverId(driverId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setOtpForDelivery("9999");
        order.setDeliveryCost(40.0);
        order.setInsuranceValue(60.0);
        order.setAppCommission(10.0);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        Wallet userWallet = new Wallet();
        userWallet.setWalletBalance(200.0);
        Wallet driverWallet = new Wallet();
        driverWallet.setWalletBalance(100.0);
        when(walletService.getWalletByUserId(userId)).thenReturn(userWallet);
        when(walletService.getWalletByUserId(driverId)).thenReturn(driverWallet);

        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));

        OrderDto res = orderService.confirmDelivery(5L, driverId, "9999");

        assertNotNull(res);
        verify(walletService, times(1)).unfreezeAmount(eq(userWallet), eq(80.0)); // deliveryCost*2
        verify(walletService, times(1)).unfreezeAmount(eq(driverWallet), eq(60.0));
        verify(walletService, times(1)).save(eq(userWallet));
        verify(walletService, times(1)).save(eq(driverWallet));
    }

//    @Test
//    public void returnOrder_unfreezesAndNotifiesUser() {
//        UUID driverId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        User driver = mock(User.class);
//        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
//        when(driver.isActive()).thenReturn(true);
//        when(driver.isDriver()).thenReturn(true);
//
//        Order order = new Order();
//        order.setId(6L);
//        order.setDriverId(driverId);
//        order.setUserId(userId);
//        order.setPickupConfirmed(true);
//        order.setDeliveryCost(50.0);
//        order.setInsuranceValue(20.0);
//        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
//
//        Wallet userWallet = new Wallet();
//        Wallet driverWallet = new Wallet();
//        when(walletService.getWalletByUserId(userId)).thenReturn(userWallet);
//        when(walletService.getWalletByUserId(driverId)).thenReturn(driverWallet);
//
//        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));
//
//        OrderDto res = orderService.returnOrder(6L, driverId);
//
//        assertNotNull(res);
//        verify(walletService, times(1)).unfreezeAmount(eq(userWallet), eq(100.0));
//        verify(walletService, times(1)).unfreezeAmount(eq(driverWallet), eq(20.0));
//        verify(whatsAppService, times(1)).sendOrderCancellation(anyString(), anyString(), anyString());
//    }

    @Test
    public void cancelOrderByDriver_resetsOrderAndSaves() {
        UUID driverId = UUID.randomUUID();
        User driver = mock(User.class);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driver.isActive()).thenReturn(true);
        when(driver.isDriver()).thenReturn(true);

        Order order = new Order();
        order.setId(7L);
        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));

        OrderDto res = orderService.cancelOrderByDriver(7L, driverId);

        assertNotNull(res);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

//    @Test
//    public void cancelOrderByUser_pending_unfreezesAndNotifies() {
//        UUID userId = UUID.randomUUID();
//        User user = mock(User.class);
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(user.isActive()).thenReturn(true);
//        when(user.isUser()).thenReturn(true);
//
//        Order order = new Order();
//        order.setId(8L);
//        order.setUserId(userId);
//        order.setStatus(OrderStatus.PENDING);
//        order.setDriverId(null);
//        order.setDeliveryCost(30.0);
//        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
//
//        Wallet userWallet = new Wallet();
//        when(walletService.getWalletByUserId(userId)).thenReturn(userWallet);
//
//        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));
//
//        when(user.getPhone()).thenReturn("+201111111111");
//        when(user.getName()).thenReturn("User One");
//
//        // Ensure the user who will be notified exists with phone/name
//        User recipient = mock(User.class);
//        when(userRepository.findById(userId)).thenReturn(Optional.of(recipient));
//        when(recipient.getPhone()).thenReturn("+209999999999");
//        when(recipient.getName()).thenReturn("Recipient");
//
//        OrderDto res = orderService.cancelOrderByUser(8L, userId, "reason");
//
//        assertNotNull(res);
//        verify(walletService, times(1)).unfreezeAmount(eq(userWallet), eq(60.0));
//        verify(whatsAppService, times(1)).sendOrderCancellation(anyString(), anyString(), anyString());
//    }

    @Test
    public void cancelOrderByUser_accepted_transfersCompensation() {
        UUID userId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.isUser()).thenReturn(true);

        Order order = new Order();
        order.setId(9L);
        order.setUserId(userId);
        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setDeliveryCost(25.0);
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        Wallet userWallet = new Wallet();
        userWallet.setWalletBalance(100.0);
        Wallet driverWallet = new Wallet();
        driverWallet.setWalletBalance(10.0);
        when(walletService.getWalletByUserId(userId)).thenReturn(userWallet);
        when(walletService.getWalletByUserId(driverId)).thenReturn(driverWallet);

        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));

        OrderDto res = orderService.cancelOrderByUser(9L, userId, null);

        assertNotNull(res);
        verify(walletService, times(1)).unfreezeAmount(eq(userWallet), eq(50.0));
        verify(walletService, times(1)).save(eq(userWallet));
        verify(walletService, times(1)).save(eq(driverWallet));
    }

    @Test
    public void updateToInTheWay_changesStatus() {
        UUID driverId = UUID.randomUUID();
        User driver = mock(User.class);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driver.isActive()).thenReturn(true);
        when(driver.isDriver()).thenReturn(true);

        Order order = new Order();
        order.setId(10L);
        order.setDriverId(driverId);
        order.setStatus(OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDto());

        OrderDto res = orderService.updateToInTheWay(10L, driverId);

        assertNotNull(res);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    public void changeOrderStatus_byAdmin_saves() {
        UUID adminId = UUID.randomUUID();
        User admin = mock(User.class);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(admin.isActive()).thenReturn(true);
        when(admin.isAdmin()).thenReturn(true);

        Order order = new Order();
        order.setId(11L);
        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDto());

        OrderDto res = orderService.changeOrderStatus(11L, OrderStatus.CANCELLED, adminId);

        assertNotNull(res);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

//    @Test
//    public void queries_returnDtosOrCounts() {
//        UUID userId = UUID.randomUUID();
//        User user = mock(User.class);
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        when(user.isActive()).thenReturn(true);
//        when(user.isUser()).thenReturn(true);
//
//        Order o1 = new Order(); o1.setUserId(userId); o1.setStatus(OrderStatus.PENDING);
//        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(java.util.List.of(o1));
//        when(orderRepository.findByDriverIdOrderByCreatedAtDesc(userId)).thenReturn(java.util.List.of(o1));
//        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)).thenReturn(java.util.List.of(o1));
//        when(orderRepository.findByUserId(userId)).thenReturn(java.util.List.of(o1));
//        when(orderRepository.findAll()).thenReturn(java.util.List.of(o1));
//
//        lenient().doReturn(new OrderDto()).when(orderMapper).toDto(any(Order.class));
//        lenient().doReturn(new DriverDto()).when(orderMapper).toDriverDto(any(Order.class));
//
//        assertFalse(orderService.getUserOrders(userId).isEmpty());
//        assertFalse(orderService.getDriverOrders(userId).isEmpty());
//        assertFalse(orderService.getAvailableOrders(userId).isEmpty());
//        assertFalse(orderService.getOrdersByUserAndStatus(userId, OrderStatus.PENDING).isEmpty());
//        when(orderRepository.findByTrackingNumber(anyString())).thenReturn(Optional.of(o1));
//        assertNotNull(orderService.getOrderByTrackingNumber("T"));
//
//        when(orderRepository.findById(1L)).thenReturn(Optional.of(o1));
//        assertNotNull(orderService.getOrderByIdAsDriverDto(1L));
//
//        OrderStatusCounts counts = orderService.getOrdersCountsByUser(userId);
//        assertEquals(1, counts.getAllOrders());
//
//        assertFalse(orderService.getAllOrders().isEmpty());
//
//        OrderStatisticsDto stats = orderService.getOrderStatistics(userId);
//        assertEquals(1, stats.getTotalOrders());
//    }
}

