package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

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
    public void combining_mark_without_composed_form_stays_invalid()
    {
        assertFalse( Xp8NodeNames.isValid( Xp8NodeNames.normalize( "q̃" ) ) );
        assertFalse( Xp8NodeNames.isValid( "กิ" ) );
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
