package com.example.coffeeorder;

import com.example.coffeeorder.domain.menu.entity.Menu;
import com.example.coffeeorder.domain.menu.repository.MenuRepository;
import com.example.coffeeorder.domain.menu.service.MenuService;
import com.example.coffeeorder.domain.order.entity.Order;
import com.example.coffeeorder.domain.order.repository.OrderRepository;
import com.example.coffeeorder.domain.order.service.OrderService;
import com.example.coffeeorder.domain.point.entity.PointHistory;
import com.example.coffeeorder.domain.point.entity.UserPoint;
import com.example.coffeeorder.domain.point.repository.PointHistoryRepository;
import com.example.coffeeorder.domain.point.repository.UserPointRepository;
import com.example.coffeeorder.domain.point.service.PointService;
import com.example.coffeeorder.domain.user.entity.User;
import com.example.coffeeorder.domain.user.repository.UserRepository;
import com.example.coffeeorder.global.exception.BusinessException;
import com.example.coffeeorder.global.exception.ErrorCode;
import com.example.coffeeorder.global.external.OrderData;
import com.example.coffeeorder.global.external.OrderDataPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
class CoffeeOrderIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserPointRepository points;
    @Autowired MenuRepository menus;
    @MockitoSpyBean PointHistoryRepository histories;
    @MockitoSpyBean OrderRepository orders;
    @Autowired PointService pointService;
    @Autowired OrderService orderService;
    @Autowired MenuService menuService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean OrderDataPlatform platform;
    @MockitoBean Clock clock;
    User user;
    Menu menu;

    @BeforeEach
    void setUp() {
        orders.deleteAll();
        histories.deleteAll();
        points.deleteAll();
        menus.deleteAll();
        users.deleteAll();
        user = users.save(new User("테스트 사용자"));
        points.save(new UserPoint(user, 10000));
        menu = menus.save(new Menu("아메리카노", 4500));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-07T03:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void contextLoads() {
        assertThat(pointService).isNotNull();
        assertThat(orderService).isNotNull();
        assertThat(menuService).isNotNull();
    }

    @Test
    void menuList() throws Exception {
        mvc.perform(get("/api/menus"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].menuId").value(menu.getId()))
                .andExpect(jsonPath("$[0].name").value("아메리카노"))
                .andExpect(jsonPath("$[0].price").value(4500));
    }

    @Test
    void chargeAndRecordHistory() throws Exception {
        mvc.perform(post("/api/users/{id}/points", user.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.balance").value(20000));
        assertThat(balance()).isEqualTo(20000);
        assertThat(histories.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getType()).isEqualTo(PointHistory.Type.CHARGE);
            assertThat(history.getAmount()).isEqualTo(10000);
            assertThat(history.getBalanceAfter()).isEqualTo(20000);
        });
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @NullSource
    void serviceRejectsInvalidCharge(Long amount) {
        assertBusiness(() -> pointService.charge(user.getId(), amount), ErrorCode.INVALID_POINT_AMOUNT);
        assertThat(balance()).isEqualTo(10000);
        assertThat(histories.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"amount\":0}", "{\"amount\":-1}", "{}", "{\"amount\":null}"})
    void apiRejectsInvalidCharge(String body) throws Exception {
        mvc.perform(post("/api/users/{id}/points", user.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POINT_AMOUNT"));
        assertThat(balance()).isEqualTo(10000);
        assertThat(histories.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.9", "4500.5"})
    void fractionalChargeIsRejected(String amount) throws Exception {
        mvc.perform(post("/api/users/{id}/points", user.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertNoOrderEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.9", "4500.5"})
    void fractionalOrderIdsAreRejected(String id) throws Exception {
        for (String body : List.of(
                "{\"userId\":" + id + ",\"menuId\":" + menu.getId() + "}",
                "{\"userId\":" + user.getId() + ",\"menuId\":" + id + "}")) {
            mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        assertNoOrderEffects();
    }

    @Test
    void chargeOverflowRollsBack() {
        assertBusiness(() -> pointService.charge(user.getId(), Long.MAX_VALUE), ErrorCode.INVALID_POINT_AMOUNT);
        assertThat(balance()).isEqualTo(10000);
        assertThat(histories.count()).isZero();
    }

    @Test
    void missingUserInCharge() throws Exception {
        mvc.perform(post("/api/users/{id}/points", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void missingUserPointInCharge() {
        points.deleteById(user.getId());
        assertBusiness(() -> pointService.charge(user.getId(), 1000L), ErrorCode.USER_POINT_NOT_FOUND);
        assertThat(histories.count()).isZero();
    }

    @Test
    void orderAndRecordPayment() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.menuId").value(menu.getId()))
                .andExpect(jsonPath("$.menuName").value("아메리카노"))
                .andExpect(jsonPath("$.amount").value(4500))
                .andExpect(jsonPath("$.remainingPoint").value(5500));
        assertThat(balance()).isEqualTo(5500);
        assertThat(orders.findAll()).singleElement().extracting(Order::getAmount).isEqualTo(4500L);
        assertThat(histories.findAll()).singleElement().satisfies(history -> {
            assertThat(history.getType()).isEqualTo(PointHistory.Type.USE);
            assertThat(history.getAmount()).isEqualTo(4500);
            assertThat(history.getBalanceAfter()).isEqualTo(5500);
        });
        verify(platform).send(new OrderData(user.getId(), menu.getId(), 4500L));
    }

    @Test
    void orderKeepsOriginalPrice() {
        var response = orderService.order(user.getId(), menu.getId());
        jdbc.update("update menus set price = ? where id = ?", 9000, menu.getId());
        assertThat(orders.findById(response.orderId()).orElseThrow().getAmount()).isEqualTo(4500);
    }

    @Test
    void missingOrderUser() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + Long.MAX_VALUE + ",\"menuId\":" + menu.getId() + "}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        assertNoOrderEffects();
    }

    @Test
    void missingOrderPoint() {
        points.deleteById(user.getId());
        assertBusiness(() -> orderService.order(user.getId(), menu.getId()), ErrorCode.USER_POINT_NOT_FOUND);
        assertThat(orders.count()).isZero();
        assertThat(histories.count()).isZero();
        verifyNoInteractions(platform);
    }

    @Test
    void missingMenu() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + user.getId() + ",\"menuId\":" + Long.MAX_VALUE + "}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        assertNoOrderEffects();
    }

    @Test
    void insufficientPoint() throws Exception {
        jdbc.update("update user_points set balance = 4000 where user_id = ?", user.getId());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_POINT"));
        assertThat(balance()).isEqualTo(4000);
        assertThat(orders.count()).isZero();
        assertThat(histories.count()).isZero();
        verifyNoInteractions(platform);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"userId\":0,\"menuId\":1}", "{\"userId\":1,\"menuId\":-1}", "{"})
    void invalidOrderRequest(String body) throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertNoOrderEffects();
    }

    @Test
    void invalidPathVariable() throws Exception {
        mvc.perform(post("/api/users/abc/points").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void chargeRollsBackWhenHistorySaveFails() {
        doThrow(new IllegalStateException("history failure")).when(histories).save(any(PointHistory.class));
        assertThatThrownBy(() -> pointService.charge(user.getId(), 1000L)).isInstanceOf(IllegalStateException.class);
        assertThat(balance()).isEqualTo(10000);
        assertThat(histories.count()).isZero();
    }

    @Test
    void orderRollsBackWhenOrderSaveFails() {
        doThrow(new IllegalStateException("order failure")).when(orders).save(any(Order.class));
        assertThatThrownBy(() -> orderService.order(user.getId(), menu.getId())).isInstanceOf(IllegalStateException.class);
        assertNoOrderEffects();
    }

    @Test
    void rolledBackOrderIsNeverSent() {
        var transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.execute(status -> {
            orderService.order(user.getId(), menu.getId());
            verifyNoInteractions(platform);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertNoOrderEffects();
    }

    @Test
    void platformSeesCommittedOrderFromIndependentTransaction() {
        record CommittedState(long orderCount, long historyCount, long balance) {}
        var observed = new AtomicReference<CommittedState>();
        doAnswer(invocation -> {
            var independent = new TransactionTemplate(transactionManager);
            independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            observed.set(independent.execute(status ->
                    new CommittedState(orders.count(), histories.count(), balance())));
            return null;
        }).when(platform).send(any());
        orderService.order(user.getId(), menu.getId());
        verify(platform).send(any());
        assertThat(observed.get()).isEqualTo(new CommittedState(1, 1, 5500));
    }

    @Test
    void platformFailureDoesNotUndoCommittedOrder() {
        doThrow(new IllegalStateException("platform failure")).when(platform).send(any());
        assertThat(orderService.order(user.getId(), menu.getId()).remainingPoint()).isEqualTo(5500);
        assertThat(balance()).isEqualTo(5500);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(histories.count()).isEqualTo(1);
    }

    @Test
    void popularMenusUseInclusiveSevenDayBoundaryAndTopThree() throws Exception {
        LocalDateTime boundary = LocalDateTime.now(clock).minusDays(7);
        var second = menus.save(new Menu("라떼", 5000));
        var third = menus.save(new Menu("모카", 5500));
        var fourth = menus.save(new Menu("차", 4000));
        var oldOnly = menus.save(new Menu("과거 메뉴", 3000));
        addOrders(menu, 4, boundary.plusDays(1));
        addOrders(second, 3, boundary); // 정확히 7일 전도 포함
        addOrders(third, 2, boundary.plusDays(2));
        addOrders(fourth, 1, boundary.plusDays(3));
        addOrders(oldOnly, 10, boundary.minusSeconds(1));
        mvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].menuId").value(menu.getId()))
                .andExpect(jsonPath("$[0].orderCount").value(4))
                .andExpect(jsonPath("$[1].menuId").value(second.getId()))
                .andExpect(jsonPath("$[1].orderCount").value(3))
                .andExpect(jsonPath("$[2].menuId").value(third.getId()))
                .andExpect(jsonPath("$[2].orderCount").value(2));
    }

    @Test
    void popularTieUsesMenuIdAscending() {
        var other = menus.save(new Menu("라떼", 5000));
        addOrders(other, 1, LocalDateTime.now(clock));
        addOrders(menu, 1, LocalDateTime.now(clock));
        assertThat(menuService.findPopular()).extracting(result -> result.menuId())
                .containsExactly(menu.getId(), other.getId());
    }

    @Test
    void noOrdersMeansNoPopularMenus() throws Exception {
        mvc.perform(get("/api/menus/popular")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void concurrentTenOrdersOnlyOneSucceeds() throws Exception {
        jdbc.update("update user_points set balance = 4000 where user_id = ?", user.getId());
        jdbc.update("update menus set price = 4000 where id = ?", menu.getId());
        var results = concurrently(10, index -> {
            try {
                orderService.order(user.getId(), menu.getId());
                return "SUCCESS";
            } catch (BusinessException exception) {
                return exception.getErrorCode().name();
            }
        });
        assertThat(results).filteredOn("SUCCESS"::equals).hasSize(1);
        assertThat(results).filteredOn("INSUFFICIENT_POINT"::equals).hasSize(9);
        assertThat(balance()).isZero();
        assertThat(orders.count()).isEqualTo(1);
        assertThat(histories.count()).isEqualTo(1);
        verify(platform, times(1)).send(any());
    }

    @Test
    void concurrentChargesDoNotLoseUpdates() throws Exception {
        concurrently(10, index -> pointService.charge(user.getId(), 1000L));
        assertThat(balance()).isEqualTo(20000);
        assertThat(histories.count()).isEqualTo(10);
        assertThat(histories.findAll()).extracting(PointHistory::getBalanceAfter)
                .containsExactlyInAnyOrder(11000L, 12000L, 13000L, 14000L, 15000L,
                        16000L, 17000L, 18000L, 19000L, 20000L);
    }

    @Test
    void concurrentChargesAndOrdersShareTheSameLock() throws Exception {
        jdbc.update("update user_points set balance = 30000 where user_id = ?", user.getId());
        concurrently(10, index -> {
            if (index % 2 == 0) return pointService.charge(user.getId(), 1000L);
            return orderService.order(user.getId(), menu.getId());
        });
        assertThat(balance()).isEqualTo(12500);
        assertThat(orders.count()).isEqualTo(5);
        assertThat(histories.count()).isEqualTo(10);
        verify(platform, times(5)).send(any());
    }

    private <T> List<T> concurrently(int count, IntFunction<T> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    return action.apply(index);
                }));
            }
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void addOrders(Menu selected, int count, LocalDateTime createdAt) {
        for (int i = 0; i < count; i++) {
            var saved = orders.save(new Order(user, selected));
            jdbc.update("update orders set created_at = ? where id = ?", createdAt, saved.getId());
        }
    }

    private long balance() { return points.findById(user.getId()).orElseThrow().getBalance(); }

    private String orderBody() {
        return "{\"userId\":" + user.getId() + ",\"menuId\":" + menu.getId() + "}";
    }

    private void assertNoOrderEffects() {
        assertThat(balance()).isEqualTo(10000);
        assertThat(orders.count()).isZero();
        assertThat(histories.count()).isZero();
        verifyNoInteractions(platform);
    }

    private void assertBusiness(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
