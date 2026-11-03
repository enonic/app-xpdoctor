package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.text.Normalizer;
import java.util.Arrays;

/**
 * Node name rules of XP 8, which are stricter than XP 7. Mirrors {@code NameValidator.NAME} in XP 8 core-internal.
 * <p>
 * Non-spacing and combining spacing marks are allowed, but a name that contains marks must be in Unicode NFC form,
 * and may not contain more than {@value #MAX_CONSECUTIVE_NON_SPACING_MARKS} consecutive non-spacing marks.
 * Enclosing marks are not allowed. Names without marks are not NFC checked.
 */
final class Xp8NodeNames
{
    private static final String ILLEGAL_CHARACTERS = "/\\|?*";

    private static final int MAX_CONSECUTIVE_NON_SPACING_MARKS = 3;

    // Sorted for binary search
    private static final int[] VALID_CHAR_TYPES =
        new int[]{Character.LOWERCASE_LETTER, Character.MODIFIER_LETTER, Character.UPPERCASE_LETTER, Character.TITLECASE_LETTER,
            Character.OTHER_LETTER, Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.DECIMAL_DIGIT_NUMBER,
            Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION, Character.DASH_PUNCTUATION, Character.CONNECTOR_PUNCTUATION, Character.OTHER_PUNCTUATION,
            Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.MATH_SYMBOL, Character.OTHER_SYMBOL};

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
        return invalidReason( name ) == null;
    }

    /**
     * @return why the name is not valid in XP 8, or {@code null} if it is valid
     */
    static String invalidReason( final String name )
    {
        if ( name.isEmpty() )
        {
            return "name is empty";
        }
        if ( name.startsWith( " " ) )
        {
            return "name starts with a space";
        }
        if ( name.endsWith( " " ) )
        {
            return "name ends with a space";
        }
        if ( containsMark( name ) && !Normalizer.isNormalized( name, Normalizer.Form.NFC ) )
        {
            return "name is not Unicode NFC normalized";
        }
        if ( exceedsConsecutiveNonSpacingMarks( name ) )
        {
            return "name contains more than " + MAX_CONSECUTIVE_NON_SPACING_MARKS + " consecutive non-spacing marks";
        }
        return name.codePoints()
            .filter( Xp8NodeNames::isInvalidChar )
            .mapToObj( cp -> "name contains character " + toUCode( cp ) + " not allowed in XP 8" )
            .findFirst()
            .orElse( null );
    }

    private static String toUCode( final int cp )
    {
        if ( cp >= 0x20 && cp <= 0x7E )
        {
            return "'" + (char) cp + "'";
        }
        return String.format( "U+%04X", cp );
    }

    private static boolean containsMark( final String name )
    {
        return name.codePoints().anyMatch( cp -> {
            final int type = Character.getType( cp );
            return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
        } );
    }

    private static boolean exceedsConsecutiveNonSpacingMarks( final String name )
    {
        int run = 0;
        for ( final int cp : name.codePoints().toArray() )
        {
            run = Character.getType( cp ) == Character.NON_SPACING_MARK ? run + 1 : 0;
            if ( run > MAX_CONSECUTIVE_NON_SPACING_MARKS )
            {
                return true;
            }
        }
        return false;
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
