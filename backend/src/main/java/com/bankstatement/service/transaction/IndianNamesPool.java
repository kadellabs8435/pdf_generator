package com.bankstatement.service.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Reusable Indian personal names for Kotak UPI/IMPS/NEFT narrations (FinBox-accepted diversity). */
final class IndianNamesPool {

    private static final String[] NAMES = {
            "Ajay Farooqui", "Sonali Malik", "Imran Sharma", "Sachin Nair", "Rajesh Joshi",
            "Khalid Mittal", "Pankaj Saxena", "Neha Kulkarni", "Swati More", "Anita Joshi",
            "Meena Srivastava", "Vikram Desai", "Priya Menon", "Arjun Reddy", "Kavita Iyer",
            "Rohit Verma", "Deepa Nambiar", "Sanjay Pillai", "Lata Agarwal", "Manoj Bhat",
            "Nitin Rao", "Shalini Dutta", "Farhan Qureshi", "Geeta Kapoor", "Harsh Mehta",
            "Irfan Sheikh", "Jyoti Pandey", "Karan Gill", "Leela Sinha", "Mohan Das",
            "Naveen Chawla", "Omkar Patil", "Pooja Hegde", "Qadir Ansari", "Ritu Shah",
            "Sameer Khanna", "Tanya Bose", "Uday Malhotra", "Varun Tiwari", "Waheeda Khan",
            "Xavier D'Souza", "Yasmin Begum", "Zaid Hussain", "Aarti Mishra", "Bhavesh Solanki",
            "Chitra Rangan", "Dinesh Yadav", "Esha Banerjee", "Faisal Ahmed", "Gauri Nair",
            "Hemant Kulkarni", "Indira Devi", "Javed Ali", "Komal Jain", "Lokesh Dubey",
            "Mamta Singh", "Naresh Gupta", "Ojaswi Roy", "Prakash Meena", "Rashmi Choudhury",
            "Sandeep Rawat", "Trisha Gowda", "Umesh Pawar", "Vidya Krishnan", "Wasim Akram",
            "Xenia Paul", "Yogesh Thakur", "Zara Sheikh", "Abhishek Dutta", "Bina Thomas",
            "Chetan Joshi", "Divya Iyer", "Ehsan Mirza", "Fatima Khan", "Gopal Reddy",
            "Heena Shaikh", "Ishaan Bose", "Juhi Agarwal", "Kunal Shah", "Lakshmi Nair",
            "Mahesh Pillai", "Nandini Rao", "Omar Siddiqui", "Pallavi Deshmukh", "Qamaruddin Khan",
            "Rahul Saxena", "Smita Patil", "Tarun Mehta", "Urvashi Joshi", "Vinod Kumar",
            "Waseem Malik", "Ximena D'Silva", "Yamini Reddy", "Zeeshan Ansari", "Amanpreet Kaur",
            "Bharat Verma", "Chandan Roy", "Diya Menon", "Ekta Sharma", "Firoz Khan",
            "Gitanjali Das", "Harish Chandra", "Iqbal Singh", "Jaya Lakshmi", "Kirti Bansal",
            "Lalit Mohan", "Monika Sethi", "Nazia Parveen", "Om Prakash", "Parul Arora",
            "Raghavendra Rao", "Shweta Ghosh", "Tejas Patwardhan", "Uma Shankar", "Vivek Anand",
            "Wahid Hussain", "Yash Pal", "Zubair Ahmed", "Aditi Chatterjee", "Brijesh Trivedi",
            "Charu Saxena", "Devendra Singh", "Ekta Verma", "Farooq Abdullah", "Gayatri Devi"
    };

    private IndianNamesPool() {}

    static List<String> shuffled(Random random) {
        List<String> list = new ArrayList<>(List.of(NAMES));
        Collections.shuffle(list, random);
        return list;
    }

    static String pickName(Random random) {
        return uppercaseForNarration(NAMES[random.nextInt(NAMES.length)]);
    }

    static String uppercaseForNarration(String name) {
        return name.toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
