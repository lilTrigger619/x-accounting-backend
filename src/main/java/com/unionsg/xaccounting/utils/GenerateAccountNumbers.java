package com.unionsg.xaccounting.utils;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GenerateAccountNumbers {
    private static final SecureRandom random = new SecureRandom();

    public static String generateAccountNumber(){
        // Format: ACC & YYYYMM & XXXXXX
        String prefix = "ACC";
        String timestamp =  new SimpleDateFormat("yyyyMM").format(new Date());
        String randomPart = String .format("%06d", random.nextInt(1000000));

        return prefix + timestamp + randomPart;
    }
}