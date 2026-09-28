package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.text.Normalizer;
import java.util.Arrays;

/**
 * Node name rules of XP 8, which are stricter than XP 7. Mirrors {@code NameValidator.NAME} in XP 8 core-internal.
 */
final class Xp8NodeNames
{
    private static final String ILLEGAL_CHARACTERS = "/\\|?*";

    // Sorted for binary search
    private static final int[] VALID_CHAR_TYPES =
        new int[]{Character.LOWERCASE_LETTER, Character.MODIFIER_LETTER, Character.UPPERCASE_LETTER, Character.TITLECASE_LETTER,
            Character.OTHER_LETTER, Character.DECIMAL_DIGIT_NUMBER, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.DASH_PUNCTUATION,
            Character.CONNECTOR_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL,
            Character.MATH_SYMBOL, Character.OTHER_SYMBOL};

    static
    {
        Arrays.sort( VALID_CHAR_TYPES );
    }

    private Xp8NodeNames()
    {
    }

    static String normalize( final String name )
    {
        return Normalizer.normalize( name, Normalizer.Form.NFC );
    }

    static boolean isValid( final String name )
    {
        if ( name.isEmpty() || name.startsWith( " " ) || name.endsWith( " " ) )
        {
            return false;
        }
        // XP 8 checks UTF-16 chars, not code points: characters outside the BMP are rejected as surrogates
        return name.chars().noneMatch( Xp8NodeNames::isInvalidChar );
    }

    private static boolean isInvalidChar( final int c )
    {
        if ( ILLEGAL_CHARACTERS.indexOf( c ) >= 0 )
        {
            return true;
        }
        if ( c == ' ' )
        {
            return false;
        }
        return Character.isWhitespace( c ) || Arrays.binarySearch( VALID_CHAR_TYPES, Character.getType( c ) ) < 0;
    }
}
