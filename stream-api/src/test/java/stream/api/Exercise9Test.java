package stream.api;

import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;

import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import common.test.tool.util.CollectorImpl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link java.util.stream.Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringJoiner> supplier = () -> new StringJoiner(",", "", "");
        BiConsumer<StringJoiner, String> accumulator = StringJoiner::add;
        BinaryOperator<StringJoiner> combiner = null;
        Function<StringJoiner, String> finisher = StringJoiner::toString;

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link java.util.stream.Collector} which can create a {@link java.util.Map} with keys as item and
         * values as {@link java.util.Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = HashMap::new;
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer) ->
            customer.getWantToBuy().forEach(
                item -> {
                    if (map.containsKey(item.getName())) {
                        map.get(item.getName()).add(customer.getName());
                    } else {
                        map.put(item.getName(), Stream.of(customer.getName()).collect(Collectors.toSet()));
                    }
                }
            );
        BinaryOperator<Map<String, Set<String>>> combiner = (left, right) -> {
            right.forEach(
                (item, customers) ->
                    left.merge(item, customers, (oldVal, newVal) -> {
                        oldVal.addAll(newVal);
                        return oldVal;
                    })
            );
            return left;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }
}