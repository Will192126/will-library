package com.will.library.log;

public class HiStackTraceFormater implements HiLogFormater<StackTraceElement[]> {
    @Override
    public String format(StackTraceElement[] data) {
        StringBuilder builder = new StringBuilder();
        if (data == null || data.length == 0) {
            return null;
        } else if (data.length == 1) {
            return "\t- " + data[0].toString();
        } else {
            for (int i = 0, len = data.length; i < len; i++) {
                if (i == 0) {
                    builder.append("stackTrace: \n");
                }
                if (i != len - 1) {
                    builder.append("\t├ ");
                    builder.append(data[i].toString());
                    builder.append("\n");
                } else {
                    builder.append("\t└ ");
                    builder.append(data[i].toString());
                }
            }
        }

        return builder.toString();
    }
}
