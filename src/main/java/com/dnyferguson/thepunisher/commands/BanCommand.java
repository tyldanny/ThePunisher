package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class BanCommand implements CommandExecutor {

    private ThePunisher plugin;

    public BanCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.ban")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /ban (username/uuid/ip) (reason)."));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");
        String targetType = plugin.getSql().getTargetType(target);
        String[] argList = Arrays.copyOfRange(args, 1, args.length);
        String reason = String.join(" ", argList);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        addBan(sender, target, targetType, reason, punisherIgn, punisherUuid);
        return true;
    }

    private void addBan(CommandSender sender, String target, String targetType, String reason, String punisherIgn, String punisherUUID) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    // Check if already banned
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bans` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        sender.sendMessage(Chat.format("&cPlayer is already banned."));
                        return;
                    }

                    pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                        ip = rs.getString("ip");

                        // Apply ban
                        sender.sendMessage(Chat.format("&aSuccessfully banned " + target + " for " + reason + "!"));
                    }

                    pst = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                            " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL," +
                            " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, NULL, '" + ip + "', '', '', NULL)");
                    pst.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
