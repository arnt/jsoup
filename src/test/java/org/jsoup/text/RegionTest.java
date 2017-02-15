package org.jsoup.text;

import java.util.List;

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
 *  Region tests.
 */
public class RegionTest {
    @Test
    public void findsSimpleRegion() {
        Document html = Jsoup.parse("<p>Three words here");
        Region words = html.find("words").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().getWholeText());
    }

    @Test
    public void findsParagraphSpanningRegion() {
        Document html = Jsoup.parse("<p>Four words<p>about love.");
        Region words = html.find("words about").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().text());
        assertEquals("about", words.getEnd().getTextNode().text());
        assertEquals("words about", words.getText());
    }

    @Test
    public void findsBrSpanningRegion() {
        Document html = Jsoup.parse("<p>Four words<br>about love.");
        Region words = html.find("words about").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().text());
        assertEquals("about", words.getEnd().getTextNode().text());
        assertEquals("words about", words.getText());
        // and for bonus harshness, try it on a Document with fewer
        // spaces than what Jsoup.parse() produces.
        for(Element p : html.select("p"))
            for(TextNode t : p.textNodes())
                t.text(t.text());
        for(Element p : html.select("br"))
            for(TextNode t : p.textNodes())
                t.text(t.text());
        assertEquals(1, html.find("words about").size());
    }

    @Test
    public void findsPartlyBoldText() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region words = html.find("emboldened").first();
        assertNotNull(words);
        assertEquals("em", words.getStart().getTextNode().text());
        assertEquals("ened", words.getEnd().getTextNode().text());
        assertEquals("emboldened", words.getText());

        assertEquals(0, html.find("em bold").size());
    }

    @Test
    public void findsProSHyCessShyedWords() {
        Document html = Jsoup.parse("<p>Cleverly pro&shy;cess&shy;ed text here");
        Region processed = html.find("processed").first();
        assertEquals("processed",
                     processed.getText().replace("\u00AD", ""));
    }


    @Test
    public void splitsAtElementBoundary() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region words = html.find("emboldened").first();
        assertNotNull(words);
        Regions parts = words.splitByElements();
        assertEquals(3, parts.size());
        assertEquals("em", parts.first().getText());
        assertEquals("bold", parts.get(1).getText());
        assertEquals("ened", parts.get(2).getText());
    }

    @Test
    public void handlesEmptyRegion() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region emboldened = html.find("emboldened").first();
        assertNotNull(emboldened);
        Region empty = new Region(emboldened.getStart(), emboldened.getStart());
        assertEquals(0, empty.splitByElements().size());
        assertEquals(0, empty.splitByBlockElements().size());
        empty.trim();
        assertEquals(emboldened.getStart(), empty.getStart());
        assertEquals(emboldened.getStart(), empty.getEnd());
        assertEquals("", empty.getText());

        Region dot = html.find(".").first();
        // next assertion breaks if the region doesn't end at the end
        // of the document
        assertEquals(dot.getEnd(), dot.getEnd().rightBound());
        empty = new Region(dot.getEnd(), dot.getEnd());
        assertEquals(0, empty.splitByElements().size());
        assertEquals(0, empty.splitByBlockElements().size());
        empty.trim();
        assertEquals(empty.getEnd(), empty.getStart());
        assertEquals("", empty.getText());
    }

    @Test
    public void entireElementRegions() {
        Document html = Jsoup.parse("<p><i>Partly em<b>bold</b></i>ened blah.");
        Element i = html.select("i").first();
        assertEquals("Partly embold", new Region(i).getText());
        assertEquals("bold", new Region(html.select("b").first()).getText());
        i.after("<div><img></div>");
        Region div = new Region(html.select("div").first());
        assertEquals("", div.getText());
        assertNotEquals(0, html.select("div").first().textNodes().size());
    }

    @Test
    public void splitsTextNodes() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();

        Region emboldened = html.find("emboldened").first();
        emboldened.splitTextNodes();
        assertEquals("emboldened", emboldened.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region four = html.find("Four").first();
        four.splitTextNodes();
        assertEquals("Four", four.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region partly = html.find("partly").first();
        assertEquals("partly", partly.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region old = html.find("old").first();
        old.splitTextNodes();
        assertEquals("old", old.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());
    }


    @Test
    public void findsExternalParent() {
        Document html = Jsoup.parse("<p>Three <i>for<b>matte</b>d</i> words.");
        Element p = html.select("p").first();
        Element i = html.select("i").first();
        assertEquals(p, p.find("Three").first().parentElement());
        assertEquals(i, p.find("matte").first().parentElement());
        assertEquals(i, p.find("ormatted").first().parentElement());
        assertEquals(p, p.find("formatted").first().parentElement());
    }


    @Test
    public void splitsSpans() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("olden").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     html.select("p").first().outerHtml());

        p.find("old").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     p.outerHtml());

        Region partly = p.find("partly").first();
        partly.ensureRemovability();
        assertEquals("partly", partly.getStart().getTextNode().text());
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     p.outerHtml());
    }

    @Test
    public void removesMinimally() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        Region partly = p.find("partly").first();
        assertEquals(p, partly.parentElement());
        assertEquals(1, partly.parents().size());
        assertEquals("partly", partly.parents().get(0).toString());
        partly.remove();
        assertEquals("<p>Four em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        p.find("bold").remove();
        assertEquals("<p>Four emened words.</p>",
                     p.outerHtml());
    }

    @Test
    public void wraps() {
        Document html = Jsoup.parse("<p>Partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("Partly").first().wrap("<i>");
        assertEquals("<p><i>Partly</i> em<b>bold</b>ened words.</p>",
                     p.outerHtml());
        p.find("olden").first().wrap("<i>");
        assertEquals("<p><i>Partly</i> em<b>b</b><i><b>old</b>en</i>ed words.</p>",
                     p.outerHtml());
        html = Jsoup.parse("<p>Partly <b>em</b>bold<b>ened</b> words.");
        p = html.select("p").first();
        p.find("emboldened").first().wrap("<i>");
        assertEquals("<p>Partly <i><b>em</b>bold<b>ened</b></i> words.</p>",
                     p.outerHtml());
        html = Jsoup.parse("<p>Partly <b>em</b>bold<b>ened</b> words.");
        p = html.select("p").first();
        p.find("mbolde").wrap("<i>");
        assertEquals("<p>Partly <b>e</b><i><b>m</b>bold<b>e</b></i><b>ned</b> words.</p>",
                     p.outerHtml());
    }

    @Test
    public void findEmptyNeedle() {
        // An empty needle previously caused an infinite loop and OutOfMemoryError
        // (line 1613 of Element.find) because regionOrNull returns a zero-length
        // region at every position, and findNext always re-finds the same position.
        Document html = Jsoup.parse("<p>Some text here");
        assertEquals(0, html.find("").size());
        assertEquals(0, html.select("p").first().find("").size());
    }

    @Test
    public void findNeedleNotPresent() {
        Document html = Jsoup.parse("<p>Some text here");
        assertEquals(0, html.find("xyzzy").size());
        assertNull(html.find("xyzzy").first());
    }

    @Test
    public void isBlankRegion() {
        Document html = Jsoup.parse("<p>word <b> </b> word");
        Region word = html.find("word").first();
        assertFalse(word.isBlank());

        // a region whose getText() yields only whitespace is blank
        Element b = html.select("b").first();
        Region spaceInBold = new Region(b);
        assertTrue(spaceInBold.isBlank());
    }

    @Test
    public void spansMultipleElementsAndBlocks() {
        Document html = Jsoup.parse("<p>em<b>bold</b>ened <p>second");

        // "emboldened" spans multiple inline elements but not block elements
        Region emboldened = html.find("emboldened").first();
        assertNotNull(emboldened);
        assertTrue(emboldened.spansMultipleElements());
        assertFalse(emboldened.spansMultipleBlockElements());

        // "ened second" spans a block element boundary (<p>)
        Region crossBlock = html.find("ened second").first();
        assertNotNull(crossBlock);
        assertTrue(crossBlock.spansMultipleElements());
        assertTrue(crossBlock.spansMultipleBlockElements());

        // "bold" is within a single inline element
        Region bold = html.find("bold").first();
        assertNotNull(bold);
        assertFalse(bold.spansMultipleElements());
        assertFalse(bold.spansMultipleBlockElements());
    }

    @Test
    public void splitsByBlockElements() {
        Document html = Jsoup.parse("<p>end of first<p>start of second");
        Region crossBlock = html.find("end of first start of second").first();
        assertNotNull(crossBlock);

        Regions byBlock = crossBlock.splitByBlockElements();
        assertEquals(2, byBlock.size());
        assertEquals("end of first", byBlock.get(0).getText());
        assertEquals("start of second", byBlock.get(1).getText());

        // splitByElements on the same cross-block region also gives 2 parts
        html = Jsoup.parse("<p>end of first<p>start of second");
        crossBlock = html.find("end of first start of second").first();
        Regions byElem = crossBlock.splitByElements();
        assertEquals(2, byElem.size());
    }

    @Test
    public void parentElementsList() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");

        Region bold = html.find("bold").first();
        List<Element> bolds = bold.parentElements();
        assertEquals(1, bolds.size());
        assertEquals("b", bolds.get(0).tagName());

        Region partly = html.find("partly").first();
        List<Element> partlys = partly.parentElements();
        assertEquals(1, partlys.size());
        assertEquals("span", partlys.get(0).tagName());

        // a region whose siblings include both Elements and TextNodes:
        // each TextNode is replaced by its parent Element (here, the <p>).
        Region emboldened = html.find("emboldened").first();
        List<Element> elems = emboldened.parentElements();
        assertEquals(3, elems.size());
        assertEquals("span", elems.get(0).tagName());
        assertEquals("b", elems.get(1).tagName());
        assertEquals("span", elems.get(2).tagName());
    }

    @Test
    public void addsAndRemovesClass() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();

        p.find("bold").first().addClass("hl");
        assertEquals("<p>Four partly em<b class=\"hl\">bold</b>ened words.</p>",
                     p.outerHtml());

        p.find("bold").first().removeClass("hl");
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        // a region whose parent Element extends past the region
        // splits the parent so that the affected Element covers
        // only the region's text.
        Element body = p.parent();
        p.find("emboldened").first().addClass("hl");
        assertEquals("<p>Four partly <span class=\"hl\">em</span><b class=\"hl\">bold</b><span class=\"hl\">ened</span> words.</p>",
                     TextUtil.stripNewlines(body.html()));
    }

    @Test
    public void parentElementsListsDisjoint() {
        Document html = Jsoup.parse("<div>ab<span>cd</span>ef</div>");
        Region abc = html.find("abc").first();
        Region def = html.find("def").first();
        assertNotNull(abc);
        assertNotNull(def);

        // Each parentElements() list covers exactly its Region's
        // text, so the two lists share no Element.
        List<Element> abcParents = abc.parentElements();
        List<Element> defParents = def.parentElements();
        for(Element e : abcParents)
            assertFalse(defParents.contains(e),
                        "Element " + e.outerHtml() + " is in both lists");
    }

    @Test
    public void trims() {
        Document html = Jsoup.parse("<p>Some partly <b>bold</b> words about life.");
        Element p = html.select("p").first();
        Region all = p.find("partly bold words").first();
        Region bold = p.find("bold").first();
        Region prelude = new Region(all.getStart(), bold.getStart());
        Region postlude = new Region(bold.getEnd(), all.getEnd());
        assertEquals("partly bold words", all.getText());
        assertEquals("partly", prelude.getText());
        assertEquals("words", postlude.getText());
        prelude.trim();
        assertEquals("partly", prelude.getText());
        postlude.trim();
        assertEquals("words", postlude.getText());
        assertEquals("partly bold words", all.getText());

        html = Jsoup.parse("<p>one <p>two");
        all = html.find("one two").first();
        assertNotNull(all);
        Region one = html.find("one").first();
        assertNotNull(one);
        Region rest = new Region(one.getEnd(), all.getEnd());
        assertEquals("two", rest.getText());
        rest.getStart().getTextNode().text("\n\n");
        assertEquals("two", rest.getText());
        rest.trim();
        assertEquals("two", rest.getText());
        rest.getEnd().getTextNode().text("2\n\n");
        rest.trim();
        assertEquals("2", rest.getText());
    }

    @Test
    public void ensuresRemovability() {
        // Empty TextNodes outside the common parent: this used to walk
        // past the common parent up to the document root, splitting
        // body and html along the way and detaching the trailing div
        // "AFTER" from the document. splitAfterUpTo() now throws if
        // it would walk past the root.
        Document doc = Jsoup.parse("<html><body></body></html>");
        Element body = doc.body();
        body.appendChild(new TextNode(""));
        body.appendChild(new TextNode(""));
        Element d = body.appendElement("div");
        d.appendElement("div").appendText("HEAD");
        Element endDiv = d.appendElement("div");
        TextNode endTn = new TextNode("ws");
        endDiv.appendChild(endTn);
        d.appendElement("div").appendText("AFTER");
        Point start = Point.atStartOfElement(body);
        Point end = new Point(endTn, endTn.getWholeText().length());
        new Region(start, end).ensureRemovability();
        assertTrue(body.text().contains("AFTER"),
                   "expected AFTER to survive, got: [" + body.text() + "]");

        // No-op: the region exactly fills its TextNode at the start
        // of its enclosing element, so no split is needed.
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("Four").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        // No-op: the region exactly fills an inline element.
        p.find("bold").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        // Mid-text split: "partly" sits inside "Four partly em" and
        // becomes its own TextNode after the call.
        Region partly = p.find("partly").first();
        partly.ensureRemovability();
        assertEquals("partly", partly.getStart().getTextNode().text());
        assertEquals(partly.getStart().getTextNode(),
                     partly.getEnd().getTextNode());

        // Idempotent: a second call leaves the document unchanged.
        String snapshot = p.outerHtml();
        partly.ensureRemovability();
        assertEquals(snapshot, p.outerHtml());

        // Region crossing block boundaries: each surrounding <p> is
        // split so the region is a contiguous run of siblings under
        // the common parent.
        Document blocks = Jsoup.parse("<p>aa bb<p>cc dd");
        blocks.find("bb cc").first().ensureRemovability();
        assertEquals("<p>aa</p><p>bb</p><p>cc</p><p>dd</p>",
                     TextUtil.stripNewlines(blocks.body().html()));
    }
}
