import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;
import fsm.api.playback.modelling.ModelRunner;
import fsm.core.CommunicationFiniteStateMachineImpl;
import fsm.logger.XesLogger;
import fsm.scxml.SCXMLParser;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.sql.Timestamp;


public class CLIModeller {

    public static void main(String[] args) {
        String filePath = null;
        String startTimeStr = null;
        String durationStr = null;
        String savePath = "some_example.xes";
        boolean printStats = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    System.out.println("Флаги:");
                    System.out.println("  -r <path>   : Путь к SCXML файлу");
                    System.out.println("  -st <date>  : Стартовое время (формат: dd.MM.yyyy HH:mm:ss)");
                    System.out.println("  -l <dur>    : Длительность (формат: 1d 2h 30m 15s)");
                    System.out.println("  -s <path>   : Путь для сохранения XES лога");
                    System.out.println("  -p          : Вывести статистику по логу");
                    return;

                case "-r":
                    if (i + 1 >= args.length) {
                        System.out.println("Ошибка: Не указан путь к файлу для флага -r");
                        return;
                    }
                    filePath = args[++i];
                    break;

                case "-st":
                    if (i + 1 >= args.length) {
                        System.out.println("Ошибка: Не указано время для флага -st");
                        return;
                    }
                    startTimeStr = args[++i];
                    break;

                case "-l":
                    if (i + 1 >= args.length) {
                        System.out.println("Ошибка: Не указана длительность для флага -l");
                        return;
                    }
                    durationStr = args[++i];
                    break;

                case "-s":
                    if (i + 1 >= args.length) {
                        System.out.println("Ошибка: Не указан путь для сохранения для флага -s");
                        return;
                    }
                    savePath = args[++i];
                    break;

                case "-p":
                    printStats = true;
                    break;

                default:
                    System.out.println("Неизвестный флаг: " + args[i]);
                    break;
            }
        }

        if (filePath == null) {
            System.out.println("Ошибка: Не указан обязательный флаг -r (файл модели)");
            return;
        }

        SCXMLParser parser = new SCXMLParser();
        List<CommunicationFiniteStateMachineImpl> fsms;

        try {
            fsms = parser.parse(filePath);
        } catch (Exception e) {
            System.out.println("Ошибка парсинга файла: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ModelRunner modelRunner = new ModelRunner("Runner model example", true, false);
        Experiment experiment = new Experiment("Experiment", false);
        modelRunner.connectToExperiment(experiment);

        if (startTimeStr != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                Date parsedDate = sdf.parse(startTimeStr);
                modelRunner.setStartDateTime(new Timestamp(parsedDate.getTime()));
                System.out.println("Установлено время старта: " + sdf.format(parsedDate));
            } catch (ParseException e) {
                System.out.println("Ошибка формата даты. Ожидается: dd.MM.yyyy HH:mm:ss");
                return;
            }
        } else {
            modelRunner.setStartDateTime(new Timestamp(System.currentTimeMillis()));
        }

        int t = 1;
        for (CommunicationFiniteStateMachineImpl fsm : fsms) {
            modelRunner.scheduleProcess(fsm, null, fsm.getName(), new TimeSpan(t, TimeUnit.MILLISECONDS));
            t+=3;
        }

        if (durationStr != null) {
            long durationSeconds = parseDuration(durationStr);
            if (durationSeconds > 0) {
                experiment.stop(new TimeInstant(durationSeconds, TimeUnit.SECONDS));
                System.out.println("Установлена длительность моделирования: " + durationSeconds + " сек");
            }
        } else {
            experiment.stop(new TimeInstant(24, TimeUnit.HOURS));
        }

        experiment.setShowProgressBar(false);

        long start = System.currentTimeMillis();
        experiment.start();
        long end = System.currentTimeMillis();

        System.out.println("Затрачено времени на расчет: " + (end - start) / 1000.0 + " с");
        experiment.finish();

        XLog log = null;
        try {
            log = new XesLogger().createXesFile(modelRunner, savePath);
            System.out.println("Лог сохранен в: " + savePath);
        }catch (Exception e){

        }

        if (printStats && log != null) {
            printStatistics(log);
        }
    }


    private static long parseDuration(String duration) {
        long totalSeconds = 0;
        String[] parts = duration.split(" ");

        for (String part : parts) {
            try {
                String unit = part.replaceAll("[^a-zA-Z]", "").toLowerCase();
                String valueStr = part.replaceAll("[^0-9]", "");
                if (valueStr.isEmpty()) continue;

                long value = Long.parseLong(valueStr);

                switch (unit) {
                    case "d": totalSeconds += value * 86400; break;
                    case "h": totalSeconds += value * 3600; break;
                    case "m": totalSeconds += value * 60; break;
                    case "s": totalSeconds += value; break;
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга части длительности: " + part);
            }
        }
        return totalSeconds;
    }


    private static void printStatistics(XLog log) {
        System.out.println("\n=== СТАТИСТИКА ПО ЛОГУ ===");

        Map<String, Map<String, Integer>> stats = new HashMap<>();

        for (XTrace trace : log) {
            String fsmName = "Unknown";
            if (trace.getAttributes().containsKey("concept:name")) {
                fsmName = trace.getAttributes().get("concept:name").toString();
            }

            if (!stats.containsKey(fsmName)) {
                stats.put(fsmName, new HashMap<>());
            }

            for (XEvent event : trace) {
                String stateName = "Unknown";
                if (event.getAttributes().containsKey("concept:name")) {
                    stateName = event.getAttributes().get("concept:name").toString();
                }

                Map<String, Integer> fsmStats = stats.get(fsmName);
                fsmStats.put(stateName, fsmStats.getOrDefault(stateName, 0) + 1);
            }
        }

        for (Map.Entry<String, Map<String, Integer>> entry : stats.entrySet()) {
            System.out.println("Автомат: " + entry.getKey());
            for (Map.Entry<String, Integer> stateEntry : entry.getValue().entrySet()) {
                System.out.printf("  - Состояние %-20s: %d раз(а)%n", stateEntry.getKey(), stateEntry.getValue());
            }
            System.out.println("  Всего событий: " + entry.getValue().values().stream().mapToInt(Integer::intValue).sum());
            System.out.println();
        }
    }
}