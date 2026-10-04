package com.example.application.common.email;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and renders the HTML email templates under
 * src/main/resources/templates/email/<name>.html - every outgoing email's actual content lives
 * in its own template file there, not inline Java string-building, so a wording/layout change
 * never requires touching the service that triggers the send.
 *
 * Placeholders use {{name}} syntax inside the template file; render() substitutes each one from
 * the given values map. Templates are cached in memory after their first read (they're bundled
 * resources that never change at runtime, so re-reading the file on every single email would be
 * pure waste) - the underlying ClassPathResource read only happens once per template name per
 * application run.
 */
@Service
public class EmailTemplateService {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * Renders templates/email/{templateName}.html with the given placeholder values.
     * Every {{key}} in the template is replaced with values.get(key) - a placeholder with no
     * matching key is left as literal text (fails visibly rather than silently vanishing, so a
     * typo'd placeholder name is easy to spot when testing a template change).
     */
    public String render(String templateName, Map<String, String> values) {
        String template = templateCache.computeIfAbsent(templateName, this::loadTemplate);
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", escapeHtml(entry.getValue()));
        }
        return result;
    }

    /** Applied automatically to every substituted value above - a company/employee name is free text someone typed into a form, never something to trust as safe-to-embed HTML. */
    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String loadTemplate(String templateName) {
        String path = "templates/email/" + templateName + ".html";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Email template not found: " + path, e);
        }
    }
}
