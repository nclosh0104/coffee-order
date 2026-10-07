package com.example.coffeeorder.domain.order.repository;

import com.example.coffeeorder.domain.menu.dto.PopularMenuResponse;
import com.example.coffeeorder.domain.order.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            select new com.example.coffeeorder.domain.menu.dto.PopularMenuResponse(m.id, m.name, count(o.id))
            from CoffeeOrder o join o.menu m
            where o.createdAt >= :since
            group by m.id, m.name
            order by count(o.id) desc, m.id asc
            """)
    List<PopularMenuResponse> findPopular(@Param("since") LocalDateTime since, Pageable pageable);
}
