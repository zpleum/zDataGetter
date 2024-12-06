package me.zpleum.zDataGetter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.text.SimpleDateFormat;
import java.util.*;

public class ZDataGetter extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private final HashMap<UUID, Integer> playTimes = new HashMap<>();
    private final HashMap<UUID, Integer> commandsUsed = new HashMap<>();
    private final HashMap<UUID, Integer> cropsHarvested = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (getCommand("zdatagetter") != null) {
            getCommand("zdatagetter").setExecutor(this);
            this.getCommand("zdatagetter").setExecutor(this);
            Objects.requireNonNull(getCommand("zdatagetter")).setExecutor(this);
        } else {
            getLogger().severe("getCommand 'zdatagetter' is returns null ! Contact zPleum Now !!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(new PlayerActivityListener(this), this);

        String time = config.getString("schedule.time", "12:00");
        scheduleDataSend(time);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("zdatagetter")) return false;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.WHITE + "\uE23C " + ChatColor.YELLOW + "คำสั่งที่ถูกต้อง " + ChatColor.GRAY + "| " + ChatColor.RED + "/zDataGetter ( reload | sendnow )");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                sender.sendMessage(ChatColor.WHITE + "丧 " + ChatColor.GREEN + "Config reloaded successfully! ( รีโหลด Config สำเร็จ! )");
                return true;

            case "sendnow":
                if (config.getBoolean("discord.enabled", false)) {
                    sendDataToDiscord();
                    sender.sendMessage(ChatColor.WHITE + "丧 " + ChatColor.GREEN + "Data has been sent to Discord! ( ส่งข้อมูลไปยังดิสคอร์ดแล้ว! )");
                } else {
                    sender.sendMessage(ChatColor.WHITE + "丧 " + ChatColor.RED + "Discord integration is disabled in the configuration! ( การส่งข้อมูลปิดอยู่ใน Config! )");
                }
                return true;

            default:
                sender.sendMessage(ChatColor.WHITE + "丧 " + ChatColor.RED + "Invalid subcommand! ( คำสั่งรองผิดพลาด! )");
                return false;
        }
    }



    public HashMap<UUID, Integer> getPlayTimes() {
        return playTimes;
    }

    public HashMap<UUID, Integer> getCommandsUsed() {
        return commandsUsed;
    }

    public HashMap<UUID, Integer> getCropsHarvested() {
        return cropsHarvested;
    }

    public void scheduleDataSend(String time) {
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);

        long delay = calculateDelay(hour, minute);
        Bukkit.getScheduler().runTaskTimer(this, this::sendDataToDiscord, delay, 24 * 60 * 60 * 20L); // Repeat every 24 hours
    }

    public void sendDataToDiscord() {
        String summaryFormat = config.getString("discord.summaryMessageFormat");
        String detailedFormat = config.getString("discord.detailedMessageFormat");

        if (summaryFormat == null || detailedFormat == null) return;

        StringBuilder detailedStats = new StringBuilder();
        int totalPlayers = Bukkit.getOnlinePlayers().size();
        int totalPlayTime = 0;
        int totalCommands = 0;
        int totalFarming = 0;

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> randomPlayers = new ArrayList<>();
        Collections.shuffle(onlinePlayers);
        for (int i = 0; i < Math.min(5, onlinePlayers.size()); i++) {
            randomPlayers.add(onlinePlayers.get(i));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            totalPlayTime += playTimes.getOrDefault(uuid, 0);
            totalCommands += commandsUsed.getOrDefault(uuid, 0);
            totalFarming += cropsHarvested.getOrDefault(uuid, 0);
        }

        for (Player player : randomPlayers) {
            UUID uuid = player.getUniqueId();
            String playerName = player.getName();
            int playTime = playTimes.getOrDefault(uuid, 0);
            int commands = commandsUsed.getOrDefault(uuid, 0);
            int farming = cropsHarvested.getOrDefault(uuid, 0);

            detailedStats.append(String.format(
                    "✅ ชื่อผู้เล่น: %s, เวลาเล่น: %d นาที, คำสั่ง: %d ครั้ง, พืชเก็บเกี่ยว: %d ต้น\n",
                    playerName, playTime, commands, farming
            ));
        }

        String summaryMessage = summaryFormat
                .replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                .replace("{totalPlayers}", String.valueOf(totalPlayers))
                .replace("{averagePlayTime}", String.valueOf(totalPlayTime / Math.max(1, totalPlayers)))
                .replace("{totalCommandsUsed}", String.valueOf(totalCommands))
                .replace("{totalCropsHarvested}", String.valueOf(totalFarming));

        String detailedMessage = detailedFormat
                .replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                .replace("{detailedPlayerStats}", detailedStats.toString().trim());

        // ใช้คำสั่ง broadcast ของ DiscordSRV
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv broadcast " + "\"" + summaryMessage + "\"");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv broadcast " + "\"" + detailedMessage + "\"");
    }


    private long calculateDelay(int hour, int minute) {
        long now = System.currentTimeMillis();
        long target = now + (hour * 60 + minute) * 60 * 1000;
        if (target < now) target += 24 * 60 * 60 * 1000; // Add 24 hours if target is in the past
        return (target - now) / 50; // Convert to ticks
    }
}
