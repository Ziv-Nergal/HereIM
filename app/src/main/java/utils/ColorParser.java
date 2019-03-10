package utils;

public class ColorParser {

    private static final int MAX_NUMERIC_VALUE = 15777215;
    private static final int MIN_NUMERIC_VALUE = 6777215;

    public static String Pars(String stringToParse){

        int numericValue = 1;

        if(stringToParse != null){
            for (char c : stringToParse.toCharArray()) {
                numericValue *= (int)c;
            }
        }

        return generateHexColor(Math.abs(numericValue % (MAX_NUMERIC_VALUE - MIN_NUMERIC_VALUE) + MIN_NUMERIC_VALUE));
    }

    private static String generateHexColor(int numericValue) {

        final char [] hexValuesArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

        char[] hexColor = new char[7];

        hexColor[0] = '#';

        for (int i = 1; i < 7; i++) {
            hexColor[i] = hexValuesArray[numericValue & 0xf];
            numericValue >>= 4;
        }

        return new String(hexColor);
    }
}
