package stream.api;

import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.AssertUtil;

import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

;

public class Exercise1Test extends ClassicOnlineStore {

    @Test
    public void findRichCustomers() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Create a {@link java.util.stream.Stream} from customerList only including customer who has more budget than 10000.
         * Use lambda expression for Predicate and {@link java.util.stream.Stream#filter} for filtering.
         */
        Predicate<Customer> richCustomerCondition = customer -> customer.getBudget() > 10000;
        Stream<Customer> richCustomerStream = customerList.stream().filter(richCustomerCondition);

        assertTrue("Solution for Predicate should be lambda expression", AssertUtil.isLambda(richCustomerCondition));
        List<Customer> richCustomer = richCustomerStream.collect(Collectors.toList());
        assertThat(richCustomer, hasSize(2));
        assertThat(richCustomer, contains(customerList.get(3), customerList.get(7)));
    }

    @Test
    public void howOldAreTheCustomers() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Create a {@link java.util.stream.Stream} from customerList with age values.
         * Use method reference(best) or lambda expression(okay) for creating {@link java.util.function.Function} which will
         * convert {@link Customer} to {@link Integer}, and then apply it by using {@link java.util.stream.Stream#map}.
         */
        Function<Customer, Integer> getAgeFunction = Customer::getAge;
        Stream<Integer> ageStream = customerList.stream().map(getAgeFunction);

        assertTrue(AssertUtil.isLambda(getAgeFunction));
        List<Integer> richCustomer = ageStream.collect(Collectors.toList());
        assertThat(richCustomer, hasSize(10));
        assertThat(richCustomer, contains(22, 27, 28, 38, 26, 22, 32, 35, 21, 36));
    }
}