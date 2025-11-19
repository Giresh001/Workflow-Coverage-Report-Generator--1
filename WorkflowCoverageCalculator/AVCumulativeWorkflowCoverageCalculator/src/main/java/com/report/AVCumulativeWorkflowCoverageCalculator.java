//package com.report;
package main.java.com.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "AVCumulativeWorkflowCoverageCalculator", mixinStandardHelpOptions = true, description = "****** CCA Calculator ******")

public class AVCumulativeWorkflowCoverageCalculator implements Runnable {

    @Option(names = {"--jsonPath"}, description = "Workflow json folder path")
    private String path;

    @Option(names = {"--date"}, description = "Enter date in format DD-MM-YYYY")
    private String date;

    @Option(names = {"--rmvListJsonPath"}, description = "Workflow json that need to be removed from the calculation list. Format {\n" +
            "\n" +
            "  \"WorkflowsList\": [" +
            "  \"Application - value\"\n" +
            "  ]\n" +
            "}")
    private String removeWorkFlowJsonFile;

    static Set<String> exeFlows = new HashSet<>();
    static Set<String> overAllWorkFlow = new HashSet<>();
    static Set<String> notExecutedWorkFlow = new HashSet<>();
    static Set<String> removeWorkflow = new HashSet<>();
    static Map<String, String> frameworkData = new HashMap<String, String>();
    static Set<String> appID = new HashSet<>();
    static String WORK_FLOW_LIST = "WorkflowsList";
    static String NOT_EXECUTED_FLOW_LIST = "NotExecutedWorkflows";
    static String EXECUTED_FLOW_LIST = "ExecutedWorkflows";

    /**
     * @param jArray
     * @return
     */
    private static Set<String> saveData(JsonNode jArray) {
        Set<String> exeFlows = new HashSet<>();
        if (jArray != null) {
            for (JsonNode value : jArray) {
                exeFlows.add(value.asText());
            }
            return exeFlows;
        }
        return exeFlows;
    }

    private static boolean containsAnyApp(String workflow, Set<String> apps) {
        for (String a : apps) {
            if (workflow.contains(a)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        new CommandLine(new main.java.com.report.AVCumulativeWorkflowCoverageCalculator()).execute(args);
    }

    @Override
    public void run() {
        Map<String, Object> finalData = new HashMap<>();
        // Parse only json files, if key is not found skip the file
        Collection<File> jsonFileList = FileUtils.listFiles(new File(path), new String[]{"json"}, true);
        // Parse json & save executed/Skipped/Total workflow list
        for (File JsonFile : jsonFileList) {
            try {
                JsonNode testJsonFile = new ObjectMapper().readTree(JsonFile);
                JsonNode apps = testJsonFile.get("AppIDs");
                for (JsonNode app : apps) {
                    appID.add(app.asText());
                }
                JsonNode jArray = testJsonFile.get(WORK_FLOW_LIST);
                overAllWorkFlow.addAll(saveData(jArray));
                JsonNode wfList = testJsonFile.get(EXECUTED_FLOW_LIST);
                exeFlows.addAll(saveData(wfList));
                JsonNode nfList = testJsonFile.get(NOT_EXECUTED_FLOW_LIST);
                notExecutedWorkFlow.addAll(saveData(nfList));
            } catch (Exception e) {
                System.out.println("Traversing to another json file as file " + JsonFile + " . Wrong file in the directory");
            }
        }
        // Remove workflows from custom list. Format of the json file {"WorkflowsList": [...]}
        if(removeWorkFlowJsonFile!=null && new File(removeWorkFlowJsonFile).exists()){
            if(new File(removeWorkFlowJsonFile).getName().toLowerCase().contains("json")){
                try {
                    JsonNode removeDataArray = new ObjectMapper().readTree(new File(removeWorkFlowJsonFile)).get(WORK_FLOW_LIST);
                    removeWorkflow.addAll(saveData(removeDataArray));
                    overAllWorkFlow.removeAll(removeWorkflow);
                    System.out.println("***********************************************************************************\n");
                    System.out.println("Exclude from calculation:");
                    removeWorkflow.stream().forEach(System.out::println);
                } catch (Exception e) {
                    System.out.println("Please check the file path is correct or remove json is not in correct json format "+removeWorkFlowJsonFile+". Supported format format \n{\n" +
                            "  \"WorkflowsList\": [" +
                            "  \"Application - value]\n" +
                            "}");
                    throw new RuntimeException();
                }
            }
        }

        // Save data and calculate CCA%
        notExecutedWorkFlow.removeAll(exeFlows);
        Map<String, Set<String>> categories = new LinkedHashMap<>();
        categories.put("AvaApplication", new HashSet<>(Arrays.asList("AvaApplicationForMR", "AvaApplicationForCT")));
        categories.put("AVViewerApplication", new HashSet<>(Arrays.asList("AVViewerApplication")));
        categories.put("FCMRInspectionMode", new HashSet<>(Arrays.asList(
                "FCMRInspectionMode",
                "QFlowInspectionMode",
                "MappingApplication",
                "TemporalApplication",
                "SpatialApplication",
                "FindingsDashboardApplication"
        )));
        categories.put("CcaApplication", new HashSet<>(Arrays.asList("CcaApplication")));
        categories.put("FunctionalCardiacCTApplication", new HashSet<>(Arrays.asList("FunctionalCardiacCTApplication")));

        for (Map.Entry<String, Set<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            Set<String> appsInCategory = entry.getValue();

            if ("AvaApplication".equals(category)) {
                long totalCt = overAllWorkFlow.stream().filter(data -> data.startsWith("AvaApplicationForCT - ")).count();

                java.util.Set<String> executedAvaSet = exeFlows.stream()
                        .filter(data -> data.startsWith("AvaApplicationForCT - ") || data.startsWith("AvaApplicationForMR - "))
                        .map(data -> data.startsWith("AvaApplicationForMR - ")
                                ? ("AvaApplicationForCT - " + data.substring("AvaApplicationForMR - ".length()))
                                : data)
                        .collect(java.util.stream.Collectors.toSet());

                java.util.Set<String> notExecutedAvaSet = notExecutedWorkFlow.stream()
                        .filter(data -> data.startsWith("AvaApplicationForCT - ") || data.startsWith("AvaApplicationForMR - "))
                        .map(data -> data.startsWith("AvaApplicationForMR - ")
                                ? ("AvaApplicationForCT - " + data.substring("AvaApplicationForMR - ".length()))
                                : data)
                        .collect(java.util.stream.Collectors.toSet());

                // Ensure not-executed excludes any executed (after canonicalization)
                notExecutedAvaSet.removeAll(executedAvaSet);

                long executedAva = executedAvaSet.size();
                long notExecutedAva = notExecutedAvaSet.size();
                long percentage = totalCt == 0 ? 0 : Math.round(((float) executedAva / totalCt) * 100);

                System.out.println("***************************************************************\n");
                System.out.println(category + " " + percentage + "%\n");
                System.out.println("overall :" + totalCt);
                System.out.println("executed :" + executedAva);
                System.out.println("Not executed : " + notExecutedAva);
                System.out.println(category + " /Executedlist :  " + "\n");
                executedAvaSet.stream().forEach(System.out::println);
                System.out.println("\n" + category + " /Not Executed :  ");
                notExecutedAvaSet.stream().forEach(System.out::println);
                System.out.println("\n" + category + " /WorkflowList :  ");
                overAllWorkFlow.stream().filter(data -> data.startsWith("AvaApplicationForCT - ")).forEach(System.out::println);

                finalData.put(WORK_FLOW_LIST + " Count", totalCt);
                finalData.put(EXECUTED_FLOW_LIST + " Count", executedAva);
                finalData.put("Percentage :", percentage + "%");
                finalData.put(EXECUTED_FLOW_LIST, new java.util.ArrayList<>(executedAvaSet));
                finalData.put(NOT_EXECUTED_FLOW_LIST, new java.util.ArrayList<>(notExecutedAvaSet));
                try {
                    frameworkData.put(category, new ObjectMapper().writeValueAsString(finalData));
                    finalData.clear();
                } catch (JsonProcessingException e) {
                    // ToDo
                }
                continue;
            }

            long overAllWorkFlowCount = overAllWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).count();
            long executedCount = exeFlows.stream().filter(data -> containsAnyApp(data, appsInCategory)).count();
            long percentage = overAllWorkFlowCount== 0?0:Math.round(((float) executedCount / overAllWorkFlowCount) * 100);
            System.out.println("***************************************************************\n");
            System.out.println(category + " " + percentage + "%\n");
            System.out.println("overall :" + overAllWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).count());
            System.out.println("executed :" + exeFlows.stream().filter(data -> containsAnyApp(data, appsInCategory)).count());
            System.out.println("Not executed : "+notExecutedWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).count());
            System.out.println(category + " /Executedlist :  " + "\n");
            exeFlows.stream().filter(data -> containsAnyApp(data, appsInCategory)).forEach(System.out::println);
            System.out.println("\n" + category + " /Not Executed :  ");
            notExecutedWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).forEach(System.out::println);
            System.out.println("\n" + category + " /WorkflowList :  ");
            overAllWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).forEach(System.out::println);
            finalData.put(WORK_FLOW_LIST + " Count", overAllWorkFlowCount);
            finalData.put(EXECUTED_FLOW_LIST + " Count", executedCount);
            finalData.put("Percentage :", percentage + "%");
            finalData.put(EXECUTED_FLOW_LIST, exeFlows.stream().filter(data -> containsAnyApp(data, appsInCategory)).collect(Collectors.toList()));
            finalData.put(NOT_EXECUTED_FLOW_LIST, notExecutedWorkFlow.stream().filter(data -> containsAnyApp(data, appsInCategory)).collect(Collectors.toList()));
            try {
                frameworkData.put(category, new ObjectMapper().writeValueAsString(finalData));
                finalData.clear();
            } catch (JsonProcessingException e) {
                // ToDo
            }
        }

        try {
            File file = new File(path + "\\data" + java.time.LocalDate.now() + ".json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, frameworkData);
        } catch (IOException e) {
            System.out.println("Dump file cannot be created. Please check the file path");
        }
    }
}