package org.jsoup.text;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Parser;


/**
 *  Regions tests.
 */
public class RegionsTest {
    @Test
    public void removesMinimally() {
        Document html = Jsoup.parse("<p>word blah <b>word</b> blah word");
        Element p = html.select("p").first();
        p.find("word").remove();
        assertEquals("<p> blah  blah </p>", p.outerHtml());

        html = Jsoup.parse("<div><p>word <b>word</b><p><b>blah word");
        Element div = html.select("div").first();
        div.find("word blah").remove();
        assertEquals("<div><p>word </p><p><b> word</b></p></div>",
                     TextUtil.stripNewlines(div.outerHtml()));

        html = Jsoup.parse("<p>wo<span>rdwordwo</span>rd</p>");
        p = html.select("p").first();
        Regions words = p.find("rdwo");
        assertEquals(2, words.size());
        assertEquals(0, words.get(0).getStart().getTextNode().siblingIndex());
        assertEquals(1, words.get(1).getStart().getTextNode().siblingIndex());
        words.remove();
        assertEquals("word", p.html());
    }

    @Test
    public void wraps() {
        Document html = Jsoup.parse("<p>Partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("olden").first().splitByElements().wrap("<i>");
        assertEquals("<p>Partly em<b>b<i>old</i></b><i>en</i>ed words.</p>",
                     p.outerHtml());
    }

    @Test
    public void ordersSafely() {
        Document html = Jsoup.parse("<p>Partly em<b>bold</b>ened words.");
        Region art = html.find("art").first();
        Region tly = html.find("tly").first();
        Regions composite = new Regions();
        composite.add(art);
        composite.add(tly);
        assertEquals(1, composite.safelyModifiable().size());
        assertEquals("artly", composite.safelyModifiable().first().getText());

        Region bold = html.find("bold").first();
        Region ened = html.find("ened").first();
        composite.add(bold);
        composite.add(ened);
        assertEquals(3, composite.safelyModifiable().size());
        assertEquals("artly", composite.safelyModifiable().last().getText());
        assertEquals("ened", composite.safelyModifiable().first().getText());

        Region lden = html.find("lden").first();
        composite.add(lden);
        assertEquals(2, composite.safelyModifiable().size());
        assertEquals("artly", composite.safelyModifiable().last().getText());
        assertEquals("boldened", composite.safelyModifiable().first().getText());

        Region ldened_w = html.find("ldened w").first();
        composite.add(ldened_w);
        assertEquals(2, composite.safelyModifiable().size());
        assertEquals("artly", composite.safelyModifiable().last().getText());
        assertEquals("boldened w", composite.safelyModifiable().first().getText());
    }
}
