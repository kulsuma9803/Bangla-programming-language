
// কথন (Kothon) — Review ১ Demo Program
// Demonstrates: declaration (both types), assignment, arithmetic with
// precedence, string concatenation, logical/relational operators,
// nested if-else, and print.

ধরি সংখ্যা বয়স = ১৮;
ধরি সংখ্যা সীমা = ১৫ + ৩ * ২;      // precedence: ৩*২ first -> সীমা = ২১
ধরি বাক্য নাম = "রাহুল";
ধরি বাক্য বার্তা = "স্বাগতম, " + নাম;   // '+' overloaded for String concat

দেখাও(বার্তা);
দেখাও("বয়স যাচাই করা হচ্ছে...");
দেখাও(বয়স);

যদি (বয়স >= সীমা) {
    দেখাও("আপনি প্রাপ্তবয়স্ক");
    ধরি সংখ্যা বোনাস = বয়স * ২;
    দেখাও(বোনাস);
} নাহলে {
    যদি (বয়স >= ১৩ এবং বয়স < সীমা) {
        দেখাও("আপনি কিশোর/কিশোরী");
    } নাহলে {
        দেখাও("আপনি এখনো শিশু");
    }
}

যদি (না (নাম == "অজানা")) {
    দেখাও("নাম পরিচিত");
}

দেখাও("প্রোগ্রাম শেষ");
