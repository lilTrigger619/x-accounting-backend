package com.unionsg.xaccounting.config.seeders.configSeeder;

import com.unionsg.xaccounting.entity.configuration.Config;
import com.unionsg.xaccounting.entity.configuration.ConfigItem;
import com.unionsg.xaccounting.utils.ConfigUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigSeedData {

    public List<Config> getConfigs() {

        return List.of(

                paymentTypes(),
                paymentOptions(),
                discountTypes(),
                currencies(),
                units(),
                categories(),
                shippingMethods(),
                expenseCategories(),
                taxAuthorities(),
                filingFrequencies(),
                taxAppliesTo(),
                taxTransactionTypes(),
                taxGroups(),
                titles(),
                countries(),
                supplierCategories(),
                employmentTypes(),
                employeeDocumentTypes()

        );
    }

    private Config paymentTypes(){
        Config config = Config.builder()
                .configKey("payment-types")
                .title("Payment Types")
//                .description("Methods used for receiving and making payments.")
                .description("Cash, Bank Transfer, Mobile Money, Cheque, etc.")
                .itemLabel("Payment Type")
//                .showValueField(true)
//                .valueFieldLabel("Credit card")// no need for this we wont show a placeholder on a select field
//                .valueFieldPlaceholder("e.g. 30")
                .sortOrder(1)
                .build();

        config.setItems(List.of(
                item("Cash", "CASH", null, "Physical cash payments", true, 1),
                item("Bank Transfer", "BANK", null,
                        "Direct bank transfers", false, 2),
                item("Mobile Money", "MM", null,
                        "M-Pesa, Airtel Money etc.", false, 3),
                item("Cheque", "CHQ", null,
                        "Cheque payments", false, 4),

                item("Credit Card", "CC", null,
                        "Card-based payments", false, 5)
        ));

        linkItems(config);
        return config;
    }

    private Config paymentOptions() {

        Config config = Config.builder()
                .configKey("payment-options")
                .title("Payment Options")
                .description("Payment terms applied to invoices and bills. Eg. Net 30, Due on Receipt, COD")
                .itemLabel("Payment Option")
                .showValueField(true)
                .valueFieldLabel("")
                .valueFieldPlaceholder("e.g.Net 30")
                .sortOrder(2)
                .build();


        config.setItems(List.of(

                item("Due on Receipt", "DOR", "0", null, true, 1),

                item("Net 15", "NET15", "15", null, false, 2),

                item("Net 30", "NET30", "30", null, false, 3),

                item("Net 60", "NET60", "60", null, false, 4),

                item("Cash on Delivery", "COD", "0", null, false, 5)

        ));

        linkItems(config);

        return config;

    }

    private Config discountTypes() {
        Config config = Config.builder()
                .configKey("discount-types")
                .title("Discount Types")
                .description("Percentage, fixed amount, promotional discounts.")
                .itemLabel("Discount Type")
                .showValueField(true)
                .valueFieldLabel("Default Value")
                .valueFieldPlaceholder("e.g. 10 or 10%")
                .sortOrder(3)
                .build();

        config.setItems(List.of(
                item("Percentage", "PCT", "%", "Discount as a % of total", true, 1),
                item("Fixed Amount", "FIX", "0.00", "Flat amount off", false, 2),
                item("Volume Discount", "VOL", null, "Tiered discount by quantity", false, 3),
                item("Promotional", "PROMO", null, "Time-limited promotions", false, 4)
        ));

        linkItems(config);
        return config;
    }

    private Config currencies() {
        Config config = Config.builder()
                .configKey("currencies")
                .title("Currencies")
                .description("Supported currencies and exchange settings.")
                .itemLabel("Currency")
                .showValueField(true)
                .valueFieldLabel("Symbol")
                .valueFieldPlaceholder("e.g. $")
                .sortOrder(4)
                .build();

        config.setItems(List.of(
                item("Kenyan Shilling", "KES", "KSh", null, true, 1),
                item("US Dollar", "USD", "$", null, false, 2),
                item("Euro", "EUR", "€", null, false, 3),
                item("British Pound", "GBP", "£", null, false, 4),
                item("Ghana Cedi", "GHS", "GH₵", null, false, 5),
                item("Nigerian Naira", "NGN", "₦", null, false, 6)
        ));

        linkItems(config);
        return config;
    }


    private Config units() {
        Config config = Config.builder()
                .configKey("units")
                .title("Units of Measure")
                .description("Pieces, kilograms, hours, boxes and more.")
                .itemLabel("Unit")
                .showValueField(true)
                .valueFieldLabel("Abbreviation")
                .valueFieldPlaceholder("e.g. pcs")
                .sortOrder(5)
                .build();

        config.setItems(List.of(
                item("Pieces", "PCS", "pcs", null, true, 1),
                item("Kilogram", "KG", "kg", null, false, 2),
                item("Hour", "HR", "hr", null, false, 3),
                item("Box", "BOX", "box", null, false, 4),
                item("Litre", "L", "L", null, false, 5)
        ));

        linkItems(config);
        return config;
    }

    private Config categories() {
        Config config = Config.builder()
                .configKey("categories")
                .title("Item Categories")
                .description("Group products and services into categories.")
                .itemLabel("Category")
                .sortOrder(6)
                .build();

        config.setItems(List.of(
                item("Services", "SVC", null, null, false, 1),
                item("Products", "PRD", null, null, false, 2),
                item("Subscriptions", "SUB", null, null, false, 3)
        ));

        linkItems(config);
        return config;
    }

    private Config shippingMethods() {
        Config config = Config.builder()
                .configKey("shipping-methods")
                .title("Shipping Methods")
                .description("Delivery options offered to customers.")
                .itemLabel("Shipping Method")
                .showValueField(true)
                .valueFieldLabel("Default Cost")
                .valueFieldPlaceholder("e.g. 500")
                .sortOrder(7)
                .build();

        config.setItems(List.of(
                item("Standard Delivery", "STD", "500", null, true, 1),
                item("Express Delivery", "EXP", "1500", null, false, 2),
                item("Pickup", "PU", "0", null, false, 3)
        ));

        linkItems(config);
        return config;
    }

    private Config expenseCategories() {
        Config config = Config.builder()
                .configKey("expense-categories")
                .title("Expense Categories")
                .description("Classify business expenses for reporting.")
                .itemLabel("Expense Category")
                .sortOrder(8)
                .build();

        config.setItems(List.of(
                item("Office Supplies", "OFF", null, null, false, 1),
                item("Travel", "TRV", null, null, false, 2),
                item("Utilities", "UTL", null, null, false, 3),
                item("Marketing", "MKT", null, null, false, 4),
                item("Salaries", "SAL", null, null, false, 5)
        ));

        linkItems(config);
        return config;
    }


    private Config taxAuthorities() {
        Config config = Config.builder()
                .configKey("tax-authorities")
                .title("Tax Authorities")
                .description("Agencies that tax rates are filed with.")
                .itemLabel("Tax Authority")
                .sortOrder(9)
                .build();

        config.setItems(List.of(
                item("Ghana Revenue Authority", "GRA", null, null, true, 1),
                item("Kenya Revenue Authority", "KRA", null, null, false, 2),
                item("Internal Revenue Service", "IRS", null, null, false, 3),
                item("HM Revenue & Customs", "HMRC", null, null, false, 4),
                item("Australian Taxation Office", "ATO", null, null, false, 5)
        ));

        linkItems(config);
        return config;
    }

    private Config filingFrequencies() {
        Config config = Config.builder()
                .configKey("filing-frequencies")
                .title("Filing Frequencies")
                .description("How often a tax is filed with its authority.")
                .itemLabel("Filing Frequency")
                .sortOrder(10)
                .build();

        config.setItems(List.of(
                item("Monthly", "MONTHLY", null, null, true, 1),
                item("Quarterly", "QUARTERLY", null, null, false, 2),
                item("Bi-Annual", "BIANNUAL", null, null, false, 3),
                item("Annual", "ANNUAL", null, null, false, 4)
        ));

        linkItems(config);
        return config;
    }

    private Config taxAppliesTo() {
        Config config = Config.builder()
                .configKey("tax-applies-to")
                .title("Tax Applies To")
                .description("What kind of supply a tax rate covers.")
                .itemLabel("Applies To")
                .sortOrder(11)
                .build();

        config.setItems(List.of(
                item("Goods & Services", "BOTH", null, null, true, 1),
                item("Goods Only", "GOODS", null, null, false, 2),
                item("Services Only", "SERVICES", null, null, false, 3),
                item("Digital Products", "DIGITAL", null, null, false, 4),
                item("Imports Only", "IMPORTS", null, null, false, 5),
                item("Exports Only", "EXPORTS", null, null, false, 6)
        ));

        linkItems(config);
        return config;
    }

    private Config taxTransactionTypes() {
        Config config = Config.builder()
                .configKey("tax-transaction-types")
                .title("Tax Transaction Types")
                .description("Whether a tax rate is used on sales, purchases or both.")
                .itemLabel("Transaction Type")
                .sortOrder(12)
                .build();

        config.setItems(List.of(
                item("All Transactions", "ALL", null, null, true, 1),
                item("Sales Only", "SALES", null, null, false, 2),
                item("Purchases Only", "PURCHASES", null, null, false, 3)
        ));

        linkItems(config);
        return config;
    }

    private Config taxGroups() {
        Config config = Config.builder()
                .configKey("tax-groups")
                .title("Tax Groups")
                .description("Groups tax rates for reporting, e.g. standard, reduced, zero rated.")
                .itemLabel("Tax Group")
                .sortOrder(13)
                .build();

        config.setItems(List.of(
                item("Standard Rates", "STANDARD", null, null, false, 1),
                item("Reduced Rates", "REDUCED", null, null, false, 2),
                item("Zero Rated", "ZERO", null, null, false, 3),
                item("Exempt", "EXEMPT", null, null, false, 4)
        ));

        linkItems(config);
        return config;
    }


    private Config titles() {
        Config config = Config.builder()
                .configKey("titles")
                .title("Titles")
                .description("Salutations used for people, e.g. Mr, Mrs, Dr.")
                .itemLabel("Title")
                .sortOrder(14)
                .build();

        config.setItems(List.of(
                item("Mr", "MR", null, null, false, 1),
                item("Mrs", "MRS", null, null, false, 2),
                item("Ms", "MS", null, null, false, 3),
                item("Dr", "DR", null, null, false, 4)
        ));

        linkItems(config);
        return config;
    }

    private Config countries() {
        Config config = Config.builder()
                .configKey("countries")
                .title("Countries")
                .description("Countries used on customer and supplier addresses.")
                .itemLabel("Country")
                .sortOrder(15)
                .build();

        config.setItems(List.of(
                item("Ghana", "GH", null, null, false, 1),
                item("Nigeria", "NG", null, null, false, 2),
                item("United States", "US", null, null, false, 3),
                item("Canada", "CA", null, null, false, 4),
                item("United Kingdom", "GB", null, null, false, 5)
        ));

        linkItems(config);
        return config;
    }

    private Config supplierCategories() {
        Config config = Config.builder()
                .configKey("supplier-categories")
                .title("Supplier Categories")
                .description("Group suppliers by what they provide.")
                .itemLabel("Supplier Category")
                .sortOrder(16)
                .build();

        config.setItems(List.of(
                item("Raw Materials", "RAW_MATERIALS", null, null, false, 1),
                item("Office Supplies", "OFFICE_SUPPLIES", null, null, false, 2),
                item("Equipment", "EQUIPMENT", null, null, false, 3),
                item("Services", "SERVICES", null, null, false, 4),
                item("Utilities", "UTILITIES", null, null, false, 5),
                item("Logistics", "LOGISTICS", null, null, false, 6),
                item("Other", "OTHER", null, null, false, 7)
        ));

        linkItems(config);
        return config;
    }

    private Config employmentTypes() {
        Config config = Config.builder()
                .configKey("employment-types")
                .title("Employment Types")
                .description("Full-time, part-time, contract and other employment arrangements.")
                .itemLabel("Employment Type")
                .sortOrder(17)
                .build();

        config.setItems(List.of(
                item("Full-time", "FULL_TIME", null, null, true, 1),
                item("Part-time", "PART_TIME", null, null, false, 2),
                item("Contract", "CONTRACT", null, null, false, 3),
                item("Temporary", "TEMPORARY", null, null, false, 4),
                item("Intern", "INTERN", null, null, false, 5)
        ));

        linkItems(config);
        return config;
    }

    private Config employeeDocumentTypes() {
        Config config = Config.builder()
                .configKey("employee-document-types")
                .title("Employee Document Types")
                .description("Tags for documents attached to employee records.")
                .itemLabel("Document Type")
                .sortOrder(18)
                .build();

        config.setItems(List.of(
                item("CV / Resume", null, null, null, false, 1),
                item("National ID Card", null, null, null, false, 2),
                item("Birth Certificate", null, null, null, false, 3),
                item("Passport", null, null, null, false, 4),
                item("Academic Certificate / Diploma", null, null, null, false, 5),
                item("Employment Contract", null, null, null, false, 6),
                item("Offer Letter", null, null, null, false, 7),
                item("Reference Letter", null, null, null, false, 8),
                item("Medical Certificate", null, null, null, false, 9),
                item("Tax Document", null, null, null, false, 10),
                item("Bank Confirmation Letter", null, null, null, false, 11),
                item("Proof of Address", null, null, null, false, 12),
                item("Next of Kin Form", null, null, null, false, 13),
                item("Other", null, null, null, false, 14)
        ));

        linkItems(config);
        return config;
    }

    private ConfigItem item(
            String name,
            String code,
            String value,
            String description,
            Boolean isDefault,
            Integer order
    ) {

        return ConfigItem.builder()
                .name(name)
                .code(code)
                .value(value)
                .description(description)
                .isDefault(isDefault)
                .sortOrder(order)
                .build();

    }

    private void linkItems(Config config){
        config.getItems()
                .forEach(item -> item.setConfig(config));
    }
}