package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Xp8NodeNamesTest
{
    @Test
    public void decomposed_name_is_invalid_until_normalized()
    {
        final String decomposed = "blåbær";

        assertFalse( Xp8NodeNames.isValid( decomposed ) );
        assertEquals( "blåbær", Xp8NodeNames.normalize( decomposed ) );
        assertTrue( Xp8NodeNames.isValid( Xp8NodeNames.normalize( decomposed ) ) );
    }

    @Test
    public void composed_name_is_valid()
    {
        assertTrue( Xp8NodeNames.isValid( "blåbær" ) );
    }

    @Test
    public void nfc_name_with_marks_is_valid()
    {
        // q + combining tilde has no composed form, so the NFC form still contains the mark
        assertTrue( Xp8NodeNames.isValid( "q̃" ) );
        // Thai: letter + vowel sign (non-spacing mark)
        assertTrue( Xp8NodeNames.isValid( "กิ" ) );
    }

    @Test
    public void devanagari_is_valid()
    {
        for ( final String name : List.of( "हमें-आपकी-सहमति-चाहिए",
                                           "कुकीज़", "ट्रैकिंग",
                                           "पर्यावरण", "हिन्दी",
                                           "नमस्ते" ) )
        {
            assertTrue( name, Xp8NodeNames.isValid( name ) );
            assertEquals( name, Xp8NodeNames.normalize( name ) );
        }
    }

    @Test
    public void enclosing_mark_is_invalid()
    {
        final String enclosed = "a⃝";

        assertFalse( Xp8NodeNames.isValid( enclosed ) );
        assertFalse( Xp8NodeNames.isValid( Xp8NodeNames.normalize( enclosed ) ) );
    }

    @Test
    public void consecutive_non_spacing_marks()
    {
        assertTrue( Xp8NodeNames.isValid( "q̀́̂" ) );

        final String stacked = "q̀́̂̃";
        assertFalse( Xp8NodeNames.isValid( stacked ) );
        assertFalse( Xp8NodeNames.isValid( Xp8NodeNames.normalize( stacked ) ) );
    }

    @Test
    public void letters_with_canonical_decomposition_but_no_marks_are_valid()
    {
        // Not NFC, but contain no marks, so they are accepted as is
        for ( final String name : List.of( "Å", "Ω", "豈", "가" ) )
        {
            assertTrue( name, Xp8NodeNames.isValid( name ) );
        }
    }

    @Test
    public void emoji_is_valid()
    {
        assertTrue( Xp8NodeNames.isValid( "cat😀" ) );
        assertFalse( Xp8NodeNames.isValid( "cat\uD83D" ) );
    }

    @Test
    public void whitespace_rules()
    {
        assertTrue( Xp8NodeNames.isValid( "my file.jpg" ) );
        assertFalse( Xp8NodeNames.isValid( " file" ) );
        assertFalse( Xp8NodeNames.isValid( "file " ) );
        assertFalse( Xp8NodeNames.isValid( "my\tfile" ) );
    }

    @Test
    public void illegal_characters_are_invalid()
    {
        assertFalse( Xp8NodeNames.isValid( "a|b" ) );
        assertFalse( Xp8NodeNames.isValid( "a?b" ) );
    }
}
