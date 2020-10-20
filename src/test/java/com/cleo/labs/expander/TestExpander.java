package com.cleo.labs.expander;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class TestExpander {

  @Test
  public void testEscape() {
    assertEquals("a0b", Expander.expand("a{}b", "0"));
    assertEquals("a{}b", Expander.expand("a\\{}b", "0"));
    assertEquals("a{trim}b", Expander.expand("a\\{trim}b", "0"));
    assertEquals("a\\0b", Expander.expand("a\\\\{}b", "0"));
  }

  @Test
  public void testArgs() {
    assertEquals("a0b1cdef", Expander.expand("a{}b{}c{}d{}e{}f", "0", "1"));
    assertEquals("a0b1c2d3e4f", Expander.expand("a{}b{}c{}d{}e{}f", "0", "1", "2", "3", "4"));
    assertEquals("a0b1c2d3e4f", Expander.expand("a{}b{}c{}d{}e{}f", "0", "1", "2", "3", "4", "5", "6"));
  }

  @Test
  public void testOptionParse() {
    assertEquals("a0b", Expander.expand("a{1}b", "0"));
    assertEquals("a0b", Expander.expand("a{trim}b", "0"));
    assertEquals("a0b", Expander.expand("a{lower}b", "0"));
    assertEquals("a0b", Expander.expand("a{tolower}b", "0"));
    assertEquals("a0b", Expander.expand("a{upper}b", "0"));
    assertEquals("a0b", Expander.expand("a{toupper}b", "0"));
    assertEquals("a0b", Expander.expand("a{TRIM}b", "0"));
    assertEquals("a0b", Expander.expand("a{LOWER}b", "0"));
    assertEquals("a0b", Expander.expand("a{TOLOWER}b", "0"));
    assertEquals("a0b", Expander.expand("a{UPPER}b", "0"));
    assertEquals("a0b", Expander.expand("a{TOUPPER}b", "0"));
    assertEquals("a0b", Expander.expand("a{1,trim,TRIM,TOlower,Upper}b", "0"));
    assertEquals("a{TOUPPERx}b", Expander.expand("a{TOUPPERx}b", "0"));
  }

  @Test
  public void testOptions() {
    assertEquals("[foo]", Expander.expand("[{trim}]", "    foo    "));
    assertEquals("[foo]", Expander.expand("[{lower}]", "FOO"));
    assertEquals("[foo]", Expander.expand("[{trim,lower}]", "    FOO    "));
    assertEquals("[foo]", Expander.expand("[{TRIMTOLOWER}]", "    FOO    "));
  }

  @Test
  public void testIndex() {
    assertEquals("a6b5c3d1e1f", Expander.expand("a{6}b{5}c{3}d{}e{1}f", "1", "2", "3", "4", "5", "6"));
  }

  @Test
  public void testSubstring() {
    assertEquals("urge", Expander.expand("{[4,8]}", "hamburger"));
    assertEquals("urge", Expander.expand("{[4,-2]}", "hamburger"));
    assertEquals("urge", Expander.expand("{[4:4]}", "hamburger"));
    assertEquals("urger", Expander.expand("{[4]}", "hamburger"));
    assertEquals("rger", Expander.expand("{[5]}", "hamburger"));
    assertEquals("r", Expander.expand("{[8]}", "hamburger"));
    assertEquals("", Expander.expand("{[9]}", "hamburger"));
    assertEquals("miles", Expander.expand("{[1,6]}", "smiles"));
    assertEquals("miles", Expander.expand("{[1,-1]}", "smiles"));
    assertEquals("miles", Expander.expand("{[1:5]}", "smiles"));
    assertEquals("miles", Expander.expand("{[1]}", "smiles"));
  }

  @Test
  public void testSubstringIndirect() {
    assertEquals("urge", Expander.expand("{[{},{}]}", "hamburger", 4, 8));
    assertEquals("urge", Expander.expand("{[{},{}]}", "hamburger", "4", -2));
    assertEquals("urge", Expander.expand("{[{2}:{2}]}", "hamburger", 4));
    assertEquals("urger", Expander.expand("{[{}]}", "hamburger", 4.0));
    assertEquals("miles", Expander.expand("{[1,{}]}", "smiles", 6));
    assertEquals("miles", Expander.expand("{[{},-1]}", "smiles", 1));
    assertEquals("miles", Expander.expand("{[1:{3}]}", "smiles", 1, 5));
    assertEquals("miles", Expander.expand("{[{4}]}", "smiles", null, null, 1));
  }

  @Test
  public void testSubstringBounds() {
    assertEquals("urger", Expander.expand("{[4,10]}", "hamburger"));
    assertEquals("", Expander.expand("{[4,4]}", "hamburger"));
    assertEquals("", Expander.expand("{[4,2]}", "hamburger"));
    assertEquals("", Expander.expand("{[4:-2]}", "hamburger"));
    assertEquals("", Expander.expand("{[4:0]}", "hamburger"));
  }

  @Test
  public void testMatch() {
    assertEquals("ell", Expander.expand("{[/[aeiou](.)\\1/]}", "hello there"));
    assertEquals("ell", Expander.expand("{[/[aeiou](.)\\1/0]}", "hello there"));
    assertEquals("l", Expander.expand("{[/[aeiou](.)\\1/1]}", "hello there"));
    assertEquals("", Expander.expand("{[/[aeiou](.)\\1/2]}", "hello there"));
    assertEquals("l", Expander.expand("{[/[aeiou](?<dot>.)\\1/dot]}", "hello there"));
    assertEquals("", Expander.expand("{[/[aeiou](?<dot>.)\\1/dit]}", "hello there"));
    String FN_EXT = "^(?<fn>.*?)(?:\\.(?<ext>[^\\.]*))?$";
    assertEquals("foo.bar", Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.bar.txt", "fn"));
    assertEquals("txt", Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.bar.txt", "ext"));
    assertEquals("foo", Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.", "fn"));
    assertEquals("", Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.", "ext"));
    assertEquals("foo", Expander.expand("{[/{3}/{}]}", "foo", "fn", FN_EXT));
    assertEquals("", Expander.expand("{[/{}/{}]}", "foo", FN_EXT, "ext"));
  }

  @Test
  public void testFormat() {
    assertEquals("abc   ", Expander.expand("{%-6s}", "abc"));
    assertEquals("   abc", Expander.expand("{1,%6s}", "abc"));
    assertEquals(" 3.142e+00", Expander.expand("{%10.3e}", Math.PI));
    assertEquals("3.142E+00", Expander.expand("{1,%10.3e,trim,upper}", Math.PI));
  }

  @Test
  public void testDate() {
    Date d = new Date(1588697522346L);
    assertEquals("-Tuesday(Tue)-1588697522346", Expander.expand("-{date(EEEE'('E'\\)')}-{%d}", d, d.getTime()));
    assertEquals("2020-05-05T16:52:02.346Z", Expander.expand("{date()[GMT]}", d));
    assertEquals("Q2'20", Expander.expand("{date(QQQ''yy)}", d));
    Date now = new Date();
    String format = "YYYY.MM.dd 'at' HH:mm";
    String nowFormatted = new SimpleDateFormat(format).format(now);
    DateFormat Iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    DateFormat IsoZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    IsoZ.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertEquals(nowFormatted, Expander.expand("{now({})}", format));
    assertEquals(Iso.format(now), Expander.expand("{date()}", now));
    assertEquals(Iso.format(now), Expander.expand("{date}", now));
    assertEquals(IsoZ.format(now), Expander.expand("{date()[GMT]}", now));
  }

  @Test
  public void testConditional() {
    assertEquals("?a=b&c=d&e=f", Expander.expand("?a=b{?}&c={}{?}&e={}", "d", "f"));
    assertEquals("?a=b&e=f", Expander.expand("?a=b{?}&c={}{?}&e={}", "", "f"));
    assertEquals("?a=b", Expander.expand("?a=b{?}&c={}{?}&e={[2]}the end", "", "f"));
    assertEquals("?a=bthe end", Expander.expand("?a=b{?}&c={}{?}&e={[2]}{.}the end", "", "f"));
  }

}
