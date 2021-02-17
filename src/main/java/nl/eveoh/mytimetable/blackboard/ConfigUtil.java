/*
 * Copyright 2013 - 2014 Eveoh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.eveoh.mytimetable.blackboard;

import blackboard.data.navigation.NavigationItem;
import blackboard.persist.navigation.NavigationItemDbLoader;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.servlet.InlineReceiptUtil;
import nl.eveoh.mytimetable.apiclient.configuration.Configuration;
import nl.eveoh.mytimetable.apiclient.configuration.WidgetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Helper methods for {@link nl.eveoh.mytimetable.blackboard.service.ConfigService}.
 *
 * @author Marco Krikke
 */
public abstract class ConfigUtil {

    /**
     * Filename of the config file.
     */
    private static final String CONFIG_FILE = "config.properties";

    private static final Logger log = LoggerFactory.getLogger(ConfigUtil.class);


    /**
     * Returns the targets for the full schedule link
     *
     * @return Targets for the full schedule link
     */
    public static Map<String, String> getHrefTargets() {
        // use a LinkedHashMap to maintain the order of insertion
        Map<String, String> targets = new LinkedHashMap<String, String>();
        targets.put("_top", "_top");
        targets.put("_blank", "_blank");

        return targets;
    }

    /**
     * Saves the configuration to the configuration file.
     *
     * @param configuration Properties file with the configuration
     */
    public static void saveConfig(Configuration configuration) {
        BufferedWriter out;

        Properties properties = configuration.toProperties();

        try {
            File configDir = PlugInUtil.getConfigDirectory("evh", "mytimetable-b2");

            // Make sure the config directory exists
            configDir.mkdirs();

            out = new BufferedWriter(new FileWriter(configDir.getAbsolutePath() + "/" + CONFIG_FILE));
        } catch (Exception ex) {
            log.error("Could not open plugin configuration for saving", ex);
            return;
        }

        try {
            properties.store(out, "MyTimetable upcoming events configuration.");
        } catch (Exception ex) {
            log.error("Could not save plugin configuration", ex);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                log.error("Unknown I/O error", e);
            }
        }

    }

    /**
     * Loads the configuration from the configuration file.
     *
     * @return Properties file containing the configuration.
     */
    public static WidgetConfiguration loadConfig() {
        BufferedReader in;
        Properties properties = new Properties();

        try {
            File configDir = PlugInUtil.getConfigDirectory("evh", "mytimetable-b2");

            in = new BufferedReader(new FileReader(configDir.getAbsolutePath() + "/" + CONFIG_FILE));
        } catch (Exception ex) {
            log.error("Could not open plugin configuration for loading", ex);
            return new WidgetConfiguration();
        }

        try {
            properties.load(in);
        } catch (Exception ex) {
            log.error("Could not load plugin configuration", ex);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.error("Unknown I/O error", e);
            }
        }

        return new WidgetConfiguration(properties);
    }

    /**
     * Checks if the Blackboard user is authorized for the system administration panel.
     *
     * @param request  {@link HttpServletRequest} belonging to the current request.
     * @param response {@link HttpServletResponse} belonging to the current request.
     */
    public static void authorizeUser(HttpServletRequest request, HttpServletResponse response) {
        // Check user permissions
        try {
            PlugInUtil.authorizeForSystemAdmin(request, response);
        } catch (Throwable ex) {
            log.warn("Could not authorize user.", ex);
            throw new RuntimeException("Could not authorize user.");
        }
    }

    /**
     * Redirects the user back the the manage plugins page, with a 'configuration saved' message.
     *
     * @param response {@link HttpServletResponse} belonging to the current request.
     *
     * @throws IOException if the redirect cannot be executed.
     */
    public static void redirectUser(HttpServletResponse response) throws IOException {
        StringBuffer returnUrl = new StringBuffer("");
        // generate a final cancelUrl
        try {
            NavigationItemDbLoader niLoader = NavigationItemDbLoader.Default.getInstance();
            NavigationItem navItem = niLoader.loadByInternalHandle("admin_plugin_manage");
            returnUrl.append(navItem.getHref());
        } catch (blackboard.persist.KeyNotFoundException kE) {
            returnUrl.append("/webapps/blackboard/admin/manage_plugins.jsp");
        } catch (blackboard.persist.PersistenceException pE) {
            returnUrl.append("/webapps/blackboard/admin/manage_plugins.jsp");
        }

        returnUrl.append("?");
        returnUrl.append(InlineReceiptUtil.SIMPLE_STRING_KEY);
        returnUrl.append("=");
        returnUrl.append("Configuration saved.");
        response.sendRedirect(returnUrl.toString());
    }
}
