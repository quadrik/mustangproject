package org.mustangproject.ZUGFeRD;

import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mustangproject.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * tests the linecalculator and transactioncalculator classes
 *
 */
public class CalculationTest {

	@Test
	public void testLineCalculator_simpleAmounts_resultInValidVATAmount() {
		final IZUGFeRDExportableProduct product = new IZUGFeRDExportableProductImpl().setVatPercent(valueOf(16));
		final IZUGFeRDExportableItem currentItem = new IZUGFeRDExportableItemImpl().setPrice(valueOf(100))
				.setQuantity(TEN)
				.setProduct(product);

		final LineCalculator calculator = new LineCalculator(currentItem);

		assertEquals(valueOf(100).stripTrailingZeros(), calculator.getPrice().stripTrailingZeros());
		assertEquals(valueOf(1000).stripTrailingZeros(), calculator.getItemTotalNetAmount().stripTrailingZeros());
		assertEquals(valueOf(160).stripTrailingZeros(), calculator.getItemTotalVATAmount().stripTrailingZeros());
	}

	@Test
	public void testLineCalculatorInclusiveAllowance() {
		//This test failed with previous implementation. By rounding the totalVATAmount to 2 decimal places the result became wrong
		final IZUGFeRDExportableProduct product = new IZUGFeRDExportableProductImpl().setVatPercent(valueOf(16));
		// 10 % discount on each item
		final IZUGFeRDAllowanceCharge allowance = new IZUGFeRDAllowanceChargeImpl().setTotalAmount(valueOf(14.8730));

		final IZUGFeRDExportableItem currentItem = new IZUGFeRDExportableItemImpl().setPrice(valueOf(148.73))
				.setQuantity(valueOf(12))
				.setItemAllowances(new IZUGFeRDAllowanceCharge[]{allowance})
				.setProduct(product);

		final LineCalculator calculator = new LineCalculator(currentItem);

		assertEquals(valueOf(133.857).stripTrailingZeros(), calculator.getPrice().stripTrailingZeros());
		assertEquals(valueOf(1606.28).stripTrailingZeros(), calculator.getItemTotalNetAmount().stripTrailingZeros());
		assertEquals(valueOf(257.0048).stripTrailingZeros(), calculator.getItemTotalVATAmount().stripTrailingZeros());
	}

	@Test
	public void testLineCalculatorInclusiveAllowanceAndCharge() {
		final IZUGFeRDExportableProduct product = new IZUGFeRDExportableProductImpl().setVatPercent(valueOf(16));
		// 10 % discount on each item
		final IZUGFeRDAllowanceCharge allowance = new IZUGFeRDAllowanceChargeImpl().setTotalAmount(valueOf(14.873));
		// 20 % charge
		final IZUGFeRDAllowanceCharge charge = new IZUGFeRDAllowanceChargeImpl().setTotalAmount(valueOf(29.746));
		final IZUGFeRDExportableItem currentItem = new IZUGFeRDExportableItemImpl().setPrice(valueOf(148.73))
				.setQuantity(valueOf(12))
				.setItemAllowances(new IZUGFeRDAllowanceCharge[]{allowance})
				.setItemCharges(new IZUGFeRDAllowanceCharge[]{charge})
				.setProduct(product);

		final LineCalculator calculator = new LineCalculator(currentItem);

		assertEquals(valueOf(163.603).stripTrailingZeros(), calculator.getPrice().stripTrailingZeros());
		assertEquals(valueOf(1963.24).stripTrailingZeros(), calculator.getItemTotalNetAmount().stripTrailingZeros());
		assertEquals(valueOf(314.1184).stripTrailingZeros(), calculator.getItemTotalVATAmount().stripTrailingZeros());
	}


	@Test
	public void testTotalCalculatorGrandTotalRounding() {
		SimpleDateFormat sqlDate = new SimpleDateFormat("yyyy-MM-dd");

		BigDecimal sales_tax_percent1 = new BigDecimal(16);
		BigDecimal total_increase_percent = new BigDecimal(0.80);
		BigDecimal total_discount_percent = new BigDecimal(2.00);


		/* invoice (1st part) */

		Invoice invoice = new Invoice();
		invoice.setDocumentName("Rechnung");
		invoice.setNumber("777777");
		try {
			invoice.setIssueDate(sqlDate.parse("2020-12-31"));
			invoice.setDetailedDeliveryPeriod(sqlDate.parse("2020-12-01 - 2020-12-31".split(" - ")[0]), sqlDate.parse("2020-12-01 - 2020-12-31".split(" - ")[1]));
			invoice.setDeliveryDate(sqlDate.parse("2020-12-31"));
			invoice.setDueDate(sqlDate.parse("2021-01-15"));
		} catch (Exception e) {
			Logger.getLogger(CalculationTest.class.getName()).log(Level.SEVERE, null, e);

		}

		/* trade party (sender) */

		TradeParty sender = new TradeParty("Maier GmbH", "Musterweg 5", "11111", "Testung", "DE");
		sender.addVATID("DE2222222222");
		invoice.setSender(sender);
		/* trade party (recipient) */

		TradeParty recipient = new TradeParty("Teston GmbH" + " " + "Zentrale" + " " + "", "Testweg 5", "11111", "Testung", "DE");
		recipient.setID("111111");
		recipient.addVATID("DE111111111");
		invoice.setRecipient(recipient);
		/* item */

		Product product;
		Item item;
		BigDecimal item_increase = BigDecimal.ZERO;
		BigDecimal item_discount = BigDecimal.ZERO;

		product = new Product("AAA", "", "H84", sales_tax_percent1).setSellerAssignedID("1AAA");
		item = new Item(product, new BigDecimal("4.750"), new BigDecimal(5.00));

		item_discount = new BigDecimal("10.00");


		if (item_increase.compareTo(BigDecimal.ZERO) > 0) {
			item.addCharge(new Charge().setPercent(item_increase).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschlag"));
		}

		if (item_discount.compareTo(BigDecimal.ZERO) > 0) {
			item.addAllowance(new Allowance().setPercent(item_discount).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatt"));
		}

		invoice.addItem(item);
		product = new Product("BBB", "", "H84", sales_tax_percent1).setSellerAssignedID("2BBB");
		item = new Item(product, new BigDecimal("5.750"), new BigDecimal(4.00));
		item_discount = BigDecimal.ZERO;
		if (item_increase.compareTo(BigDecimal.ZERO) > 0) {
			item.addCharge(new Charge().setPercent(item_increase).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschlag"));
		}
		if (item_discount.compareTo(BigDecimal.ZERO) > 0) {
			item.addAllowance(new Allowance().setPercent(item_discount).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatt"));
		}

		invoice.addItem(item);
		product = new Product("CCC", "", "H84", sales_tax_percent1).setSellerAssignedID("3CCC");
		item = new Item(product, new BigDecimal("6.750"), new BigDecimal(3.00));
		item_discount = new BigDecimal("10.00");
		if (item_increase.compareTo(BigDecimal.ZERO) > 0) {
			item.addCharge(new Charge().setPercent(item_increase).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschlag"));
		}

		if (item_discount.compareTo(BigDecimal.ZERO) > 0) {
			item.addAllowance(new Allowance().setPercent(item_discount).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatt"));
		}

		invoice.addItem(item);


		product = new Product("DDD", "", "H84", sales_tax_percent1).setSellerAssignedID("4DDD");
		item = new Item(product, new BigDecimal("7.750"), new BigDecimal(2.00));

		item_discount = BigDecimal.ZERO;


		if (item_increase.compareTo(BigDecimal.ZERO) > 0) {
			item.addCharge(new Charge().setPercent(item_increase).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschlag"));
		}

		if (item_discount.compareTo(BigDecimal.ZERO) > 0) {
			item.addAllowance(new Allowance().setPercent(item_discount).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatt"));
		}

		invoice.addItem(item);


		product = new Product("EEE", "", "H84", sales_tax_percent1).setSellerAssignedID("5EEE");
		item = new Item(product, new BigDecimal("8.750"), new BigDecimal(1.00));

		item_discount = BigDecimal.ZERO;


		if (item_increase.compareTo(BigDecimal.ZERO) > 0) {
			item.addCharge(new Charge().setPercent(item_increase).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschlag"));
		}

		if (item_discount.compareTo(BigDecimal.ZERO) > 0) {
			item.addAllowance(new Allowance().setPercent(item_discount).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatt"));

		}
		invoice.addItem(item);

		if (total_increase_percent.compareTo(BigDecimal.ZERO) > 0) {
			invoice.addCharge(new Charge().setPercent(total_increase_percent).setTaxPercent(sales_tax_percent1).setCategoryCode("ZZZ").setReason("Zuschläge"));
		}

		if (total_discount_percent.compareTo(BigDecimal.ZERO) > 0) {
			invoice.addAllowance(new Allowance().setPercent(total_discount_percent).setTaxPercent(sales_tax_percent1).setCategoryCode("95").setReason("Rabatte"));
		}
		TransactionCalculator calculator = new TransactionCalculator(invoice);
		assertEquals(valueOf(99.54).stripTrailingZeros(), calculator.getGrandTotal().stripTrailingZeros());
	}


}
