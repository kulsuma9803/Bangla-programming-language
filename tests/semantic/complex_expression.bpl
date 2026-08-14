
// Advanced semantic test: full operator precedence + logical/relational chains
ধরি সংখ্যা ক = ২;
ধরি সংখ্যা খ = ৩;
ধরি সংখ্যা গ = ৪;

// Precedence: * before +, so this is ক + (খ * গ) - ২ = ২ + ১২ - ২ = ১২
ধরি সংখ্যা ফলাফল = ক + খ * গ - ২;
দেখাও(ফলাফল);

// Logical combination: relational ops combined with এবং/অথবা/না
যদি (ফলাফল > ১০ এবং ক < খ) {
    দেখাও("শর্ত সত্য");
} নাহলে {
    দেখাও("শর্ত মিথ্যা");
}

যদি (না (ক == খ)) {
    দেখাও("ক এবং খ অসমান");
}
