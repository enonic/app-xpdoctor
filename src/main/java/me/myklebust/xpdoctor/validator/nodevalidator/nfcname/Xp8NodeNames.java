package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.text.Normalizer;
import java.util.Arrays;

/**
 * Node name rules of XP 8, which are stricter than XP 7. Mirrors {@code NameValidator.NAME} in XP 8 core-internal.
 * <p>
 * Names must be in Unicode NFC form. Non-spacing (Mn) and combining spacing (Mc) marks are allowed when attached to
 * a letter or a decimal digit, with at most {@value #MAX_NON_SPACING_MARKS_PER_BASE} non-spacing marks per base
 * character. Invisible marks (Default_Ignorable_Code_Point, e.g. variation selectors) and enclosing marks (Me) are not
 * allowed.
 */
final class Xp8NodeNames
{
    private static final String ILLEGAL_CHARACTERS = "/\\|?*";

    /**
     * Maximum number of non-spacing marks (Mn) attached to a single base character.
     * Spacing marks (Mc) in the same mark sequence do not reset the count.
     */
    private static final int MAX_NON_SPACING_MARKS_PER_BASE = 4;

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
        if ( !Normalizer.isNormalized( name, Normalizer.Form.NFC ) )
        {
            return "name is not Unicode NFC normalized";
        }
        final String invalidChar = name.codePoints()
            .filter( Xp8NodeNames::isInvalidChar )
            .mapToObj( cp -> "name contains character " + toUCode( cp ) + " not allowed in XP 8" )
            .findFirst()
            .orElse( null );
        if ( invalidChar != null )
        {
            return invalidChar;
        }
        return invalidMarks( name );
    }

    /**
     * Validates combining marks (Mn, Mc). Expects an NFC string.
     * <p>
     * A mark is attached if the nearest preceding non-mark code point is a letter (L*) or a decimal digit (Nd).
     * A mark sequence inherits the attachment of its first mark.
     */
    private static String invalidMarks( final String name )
    {
        boolean attached = false;
        int nonSpacingMarks = 0;
        for ( int i = 0; i < name.length(); )
        {
            final int cp = name.codePointAt( i );
            final int type = Character.getType( cp );
            if ( isMark( type ) )
            {
                if ( isInvisibleMark( cp ) )
                {
                    return "name contains invisible mark " + toUCode( cp );
                }
                if ( !attached )
                {
                    return "name contains unattached mark " + toUCode( cp );
                }
                if ( type == Character.NON_SPACING_MARK && ++nonSpacingMarks > MAX_NON_SPACING_MARKS_PER_BASE )
                {
                    return "name contains more than " + MAX_NON_SPACING_MARKS_PER_BASE + " non-spacing marks per character";
                }
            }
            else
            {
                attached = Character.isLetter( cp ) || type == Character.DECIMAL_DIGIT_NUMBER;
                nonSpacingMarks = 0;
            }
            i += Character.charCount( cp );
        }
        return null;
    }

    private static boolean isMark( final int type )
    {
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    /**
     * Marks (Mn) that are also Default_Ignorable_Code_Point:
     * combining grapheme joiner, Khmer inherent vowels, Mongolian free variation selectors, variation selectors.
     */
    private static boolean isInvisibleMark( final int cp )
    {
        return cp == 0x034F || ( cp >= 0x17B4 && cp <= 0x17B5 ) || ( cp >= 0x180B && cp <= 0x180D ) || cp == 0x180F ||
            ( cp >= 0xFE00 && cp <= 0xFE0F ) || ( cp >= 0xE0100 && cp <= 0xE01EF );
    }

    private static String toUCode( final int cp )
    {
        if ( cp >= 0x20 && cp <= 0x7E )
        {
            return "'" + (char) cp + "'";
        }
        return String.format( "'U+%04X'", cp );
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
