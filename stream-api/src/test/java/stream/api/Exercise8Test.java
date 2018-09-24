package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.entity.Shop;

import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class Exercise8Test extends ClassicOnlineStore {

    @Difficult
    @Test
    public void itemsNotOnSale() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a set of item names that are in {@link Customer.wantToBuy} but not on sale in any shop.
         */
        List<String> itemListOnSale = shopStream.map(Shop::getItemList)
                .flatMap(List::stream)
                .map(Item::getName)
                .collect(Collectors.toList());
        Set<String> itemSetNotOnSale = customerStream.map(Customer::getWantToBuy)

                .flatMap(List::stream)
                .filter(item -> {
                    return !itemListOnSale.contains(item.getName());
                })
                .map(Item::getName)
                .collect(Collectors.toSet());

        assertThat(itemSetNotOnSale, hasSize(3));
        assertThat(itemSetNotOnSale, hasItems("bag", "pants", "coat"));
    }

    @Difficult
    @Test
    public void havingEnoughMoney() {
        Stream<Customer> customerStream = this.mall.getCustomerList().stream();
        Stream<Shop> shopStream = this.mall.getShopList().stream();

        /**
         * Create a customer's name list including who are having enough money to buy all items they want which is on sale.
         * Items that are not on sale can be counted as 0 money cost.
         * If there is several same items with different prices, customer can choose the cheapest one.
         */
        List<Item> onSale = shopStream.map(Shop::getItemList)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        //a person has enough money if the total price of the list of items they want to buy is below their budget and everything they want is available
        //to get the total price of what they want to buy, see if the item they want has different prices and choose the cheapest one

        Predicate<Customer> havingEnoughMoney = customer -> {
            return customer.getBudget() >= onSale.stream()
                    //item is present inside customer's want to buy subset
                    .filter(onSaleItem -> customer.getWantToBuy().stream()
                            .map(Item::getName).collect(Collectors.toList())
                            .contains(onSaleItem.getName()))
                    //is the cheapest possible item
                    .filter(onSaleItem -> onSaleItem.getPrice() <= onSale.stream()
                            .filter(onSaleSubItem -> onSaleSubItem.getName().equals(onSaleItem.getName()))
                            .mapToInt(Item::getPrice)
                            .min().getAsInt())
                    .mapToInt(Item::getPrice)
                    .sum();
        };

//        Predicate<Customer> havingEnoughMoneySolution = c ->
//                c.getBudget() >= c.getWantToBuy()
//                        .stream()
//                        .mapToInt(wanted -> onSale.stream()
//                                .filter(shopItem ->
//                                        shopItem.getName().equals(
//                                                wanted.getName()))
//                                .sorted((o1, o2) -> o1.getPrice() - o2.getPrice())
//                                .findFirst()
//                                .map(Item::getPrice).orElse(0))
//                        .sum();

//        List<Item> peteOnSale = shopStream.map(Shop::getItemList)
//                .flatMap(List::stream)
//                .sorted(Comparator.comparing(Item::getName)
//                        .thenComparing(Item::getPrice))
//                .collect(Collectors.groupingBy(Item::getName))
//                .entrySet()
//                .stream()
//                .map(entry -> entry.getValue().get(0))
//                .collect(Collectors.toList());
//        Predicate<Customer> peteHavingMoney = customer -> onSale.stream()
//                .filter(item ->
//                        customer.getWantToBuy().stream()
//                                .map(Item::getName)
//                                .anyMatch(item.getName()::equals))
//                .mapToLong(Item::getPrice).sum() <= customer.getBudget();


        List<String> customerNameList = customerStream.filter(havingEnoughMoney)
                .map(Customer::getName)
                .collect(Collectors.toList());


        assertThat(customerNameList, hasSize(7));
        assertThat(customerNameList, hasItems("Joe", "Patrick", "Chris", "Kathy", "Alice", "Andrew", "Amy"));
    }
}