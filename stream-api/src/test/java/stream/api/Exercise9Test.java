package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         *
         * Solution below made prior to learning about the StringJoiner class
         */
        //return container of strings
        Supplier<ArrayList<String>> supplier = ArrayList::new;
        //add strings to accumulator
        BiConsumer<ArrayList<String>, String> accumulator = (list,s)->list.add(s);
        //in case stream is processed in parallel, combine the two
        BinaryOperator<ArrayList<String>> combiner = (list1, list2)->{
            list2.forEach(s->{
                if(!list1.contains(s)) list1.add(s);
            });
            return list1;
        };
        //collect and delimit with comma
        Function<ArrayList<String>, String> finisher = list->list.stream().collect(Collectors.joining(","));

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        //intermediate container
        Supplier<Map<String, Set<String>>> supplier = ConcurrentHashMap::new;

        //put customer names into a map of items and customers wanting those items
        //putIfAbsent may return null (if put is successful), so use Optional
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer)->{
            customer.getWantToBuy().forEach(item->{
                Optional<Set<String>> set = Optional.ofNullable(
                        map.putIfAbsent(item.getName(),
                        Stream.of(customer.getName()).collect(Collectors.toSet())));
                set.ifPresent(theSet->theSet.add(customer.getName()));
            });
        };

        //to combine, put key+set of map2 in map1 if the key doesn't exist
        //if the key exists, just add set values from map2 into map1
        BinaryOperator<Map<String, Set<String>>> combiner = (map1,map2)->{
            map2.forEach((item, customers)->{
                Optional<Set<String>> set = Optional.ofNullable(map1.putIfAbsent(item,customers));
                set.ifPresent(theSet->theSet.addAll(customers));
            });
            return map1;
        };

        //already using IDENTITY_FINISH
        Function< Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

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

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        //intermediate container
        Supplier<ArrayList<Integer>> supplier = ArrayList::new;

        //put the specified bits to be ON in arraylist of integers
        BiConsumer<ArrayList<Integer>, String> accumulator = (list, bits)->{
            //dash indicates range, so add that range of numbers into arraylist, otherwise add as int
            if (bits.contains("-")) {
                int [] intRange = Stream.of(bits.split("-")).mapToInt(Integer::parseInt).toArray();
                list.addAll(IntStream.rangeClosed(intRange[0], intRange[1]).boxed().collect(Collectors.toList()));
            } else {
                list.add(Integer.parseInt(bits));
            }
        };

        //for each integer in the list, change that index in stringbuilder to ON
        Function<ArrayList<Integer>, String> finisher = list->{
            StringBuilder result = new StringBuilder(String.join("",
                    Collections.nCopies(list.stream().mapToInt(Integer::intValue).max().getAsInt(), "0")));
            list.forEach(i -> result.setCharAt(i-1,'1'));
            return result.toString();
        };

        //not worrying about combiner
        Collector<String, ?, String> toBitString =
                new CollectorImpl<>(supplier, accumulator, null, finisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
