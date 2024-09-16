/**
 * The base API package for Earth.
 * <p>
 * Obtain an instance of the EarthAPI interface in order to access the rest of the API.
 * For example:
 * </p>
 * <pre>
 * <code>
 * EarthAPI api = null;
 * Plugin earth = Bukkit.getPluginManager().getPlugin("Earth");
 * if (earth != null {@literal&}{@literal&} earth.isEnabled()) {
 *     RegisteredServiceProvider{@literal<}EarthAPI{@literal>} provider = Bukkit.getServicesManager().getRegistration(EarthAPI.class);
 *     if (provider != null) {
 *         api = provider.getProvider();
 *         Bukkit.getServer().getConsoleSender().sendMessage("Successfully enabled Earth API");
 *     } else {
 *         Bukkit.getServer().getConsoleSender().sendMessage("Failed to enable Earth API, invalid provider");
 *     }
 * } else {
 *     Bukkit.getServer().getConsoleSender().sendMessage("Failed to enable Earth API, plugin not found or disabled");
 * }
 * </code>
 * </pre>
 * 
 * @author squadsteam
 *
 */
package com.github.ssquadteam.earth.api;
