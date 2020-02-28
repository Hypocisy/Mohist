/*
 * TerminalConsoleAppender
 * Copyright (c) 2017 Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package red.mohist.console.log4j;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.util.PerformanceSensitive;
import red.mohist.configuration.MohistConfigUtil;
import red.mohist.util.ANSIColorUtils;

@Plugin(name = "highlightLevel", category = PatternConverter.CATEGORY)
@ConverterKeys({ "highlightLevel" })
@PerformanceSensitive("allocation")
public class HighlightLevelConverter extends LogEventPatternConverter
{
    private static final String ANSI_RESET = "\u001B[39;0m";
    private static final String ANSI_ERROR = geterror();
    private static final String ANSI_WARN = getwarn();

    private final List<PatternFormatter> formatters;

    /**
     * Construct the converter.
     *
     * @param formatters The pattern formatters to generate the text to highlight
     */
    protected HighlightLevelConverter(List<PatternFormatter> formatters)
    {
        super("highlightLevel", null);
        this.formatters = formatters;
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo)
    {
        Level level = event.getLevel();
        if (level.isMoreSpecificThan(Level.ERROR))
        {
            format(ANSI_ERROR, event, toAppendTo);
            return;
        }
        else if (level.isMoreSpecificThan(Level.WARN))
        {
            format(ANSI_WARN, event, toAppendTo);
            return;
        }

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = formatters.size(); i < size; i++)
        {
            formatters.get(i).format(event, toAppendTo);
        }
    }

    private void format(String style, LogEvent event, StringBuilder toAppendTo)
    {
        int start = toAppendTo.length();
        toAppendTo.append(style);
        int end = toAppendTo.length();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = formatters.size(); i < size; i++)
        {
            formatters.get(i).format(event, toAppendTo);
        }

        if (toAppendTo.length() == end)
        {
            // No content so we don't need to append the ANSI escape code
            toAppendTo.setLength(start);
        }
        else
        {
            // Append reset code after the line
            toAppendTo.append(ANSI_RESET);
        }
    }

    @Override
    public boolean handlesThrowable()
    {
        for (final PatternFormatter formatter : formatters)
        {
            if (formatter.handlesThrowable())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a new instance of the {@link HighlightLevelConverter} with the
     * specified options.
     *
     * @param config The current configuration
     * @param options The pattern options
     * @return The new instance
     */
    @Nullable
    public static HighlightLevelConverter newInstance(Configuration config, String[] options)
    {
        if (options.length != 1)
        {
            LOGGER.error("Incorrect number of options on highlightError. Expected 1 received " + options.length);
            return null;
        }
        if (options[0] == null)
        {
            LOGGER.error("No pattern supplied on highlightError");
            return null;
        }

        PatternParser parser = PatternLayout.createPatternParser(config);
        List<PatternFormatter> formatters = parser.parse(options[0]);
        return new HighlightLevelConverter(formatters);
    }

    public static String geterror() {
        File f = new File("mohist-config", "mohist.yml");
        String cc = MohistConfigUtil.getString(f,"error-level:", "c");
        return ANSIColorUtils.getColor(cc, "\u001B[31;1m");
    }

    public static String getwarn() {
        File f = new File("mohist-config", "mohist.yml");
        String cc = MohistConfigUtil.getString(f, "warn-level:", "e");
        return ANSIColorUtils.getColor(cc, "\u001B[33;1m");
    }
}
