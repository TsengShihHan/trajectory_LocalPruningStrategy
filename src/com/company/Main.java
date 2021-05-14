package com.company;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        //下面三行為記憶體計算
        /* Get the Java runtime */
        Runtime runtime = Runtime.getRuntime();
        /* Run the garbage collector */
        runtime.gc();

        UgainCount ugainCount = new UgainCount();
        final float Pbr = (float) 0.5;  //所設定的Pbr閥值
        int intoGlobal = 0;
        String inputFileName = "test.txt";  //輸入測試檔案名稱

        final HashMap<String, LinkedList<String>> trajectoryData = getTrajectoryData(inputFileName);  //取得軌跡資料檔案(txt) EX:{t4=[a2, a3, b1], t5=[a3, a1, b1], t6=[a3, a1, b1], t7=[a3, b2, a1], t8=[a3, b2, b3], t1=[a1, b2, b3], t2=[b1, a2, b2, a3], t3=[a2, b3, a3]}

        long createBipartiteGraphStartTime = System.currentTimeMillis();   //獲取建立bipartite graph的開始時間
        Bipartite bipartiteData = new Bipartite(trajectoryData);  // 取得建立bipartiteData的分群資料以及投影關聯
        long createBipartiteGraphEndTime = System.currentTimeMillis();   //獲取建立bipartite graph的結束時間

        while (true) {
            FindPP findOrgPP = new FindPP(bipartiteData.biT, bipartiteData.biCT, Pbr, 0);  //二-1、掃描異常項目 check：0初始化/1loop計算
            if (findOrgPP.problematicTotal.equals(0)) {
                break;

            }  //終止條件設定

            Map.Entry<LinkedList<String>, Float> localMaxUgainPart = findLocalMaxUgain(bipartiteData, findOrgPP, ugainCount, trajectoryData, Pbr, findOrgPP.problematicTotal);
            if (localMaxUgainPart != null) {
                if (localMaxUgainPart.getValue() > 0) {
                    LinkedList<String> localMaxUgainPartKey = localMaxUgainPart.getKey();
                    trajectoryData.get(localMaxUgainPartKey.get(0)).remove(localMaxUgainPartKey.get(1));  //依據回傳位置刪除軌跡，而位置index需轉回int型態
                    bipartiteData = new Bipartite(trajectoryData);

                } else {
                    intoGlobal = 1;
                    new Global_sup(trajectoryData, Pbr, createBipartiteGraphStartTime, createBipartiteGraphEndTime, inputFileName, ugainCount.ugain_count);
                    break;

                }
            } else {
                intoGlobal = 1;
                new Global_sup(trajectoryData, Pbr, createBipartiteGraphStartTime, createBipartiteGraphEndTime, inputFileName, ugainCount.ugain_count);
                break;

            }
        }
        long programEndTime = System.currentTimeMillis();   //獲取程式最結束終時間

        /* Calculate the used memory */
        long memory = runtime.totalMemory() - runtime.freeMemory();
        if (intoGlobal != 1) {
            System.out.println("1.建立bipartite graph的時間：" + (createBipartiteGraphEndTime - createBipartiteGraphStartTime) + "ms");
            System.out.println("2.建完bipartite graph後，一直到最終結束所花的時間：" + (programEndTime - createBipartiteGraphEndTime) + "ms");
            System.out.println("3.計算gain的總次數：" + ugainCount.ugain_count + "次");
            System.out.println("4.最後有多少 #trajectories：\t" + TrajectoriesCount(trajectoryData) + "個；");
            System.out.println("5.原始 #trajectories：\t\t" + TrajectoriesCount(getTrajectoryData(inputFileName)) + "個；");
            System.out.println("6.最後有多少 #locations：\t" + locationsCount(bipartiteData.biT) + "個；");
            System.out.println("7.原始 #locations：\t\t\t" + locationsCount(new Bipartite(getTrajectoryData(inputFileName)).biT) + "個；");

        }
        System.out.println("8.所需記憶體空間：" + memory + "bytes");

    }

    /*
     * 讀取軌跡檔案
     * */
    protected static HashMap<String, LinkedList<String>> getTrajectoryData(String inputFileName) {
        final HashMap<String, LinkedList<String>> trajectoryTale = new HashMap<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("./input/" + inputFileName), StandardCharsets.UTF_8)); // 指定讀取文件的編碼格式，以免出現中文亂碼
            String str;

            while ((str = reader.readLine()) != null) {
                trajectoryTale.put(str.split(" ")[0], new LinkedList<>(Arrays.asList(Arrays.copyOfRange(str.split(" "), 1, str.split(" ").length))));  //根據空白進行切割，第0個當作key，1~全部當作value

            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                assert reader != null;
                reader.close();

            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        return trajectoryTale;
    }

    /*
     * 加入PruningStrategy演算法找出最大gain
     * */
    private static Map.Entry<LinkedList<String>, Float> findLocalMaxUgain(Bipartite bipartiteData, FindPP findOrgPP, UgainCount ugainCount, HashMap<String, LinkedList<String>> trajectoryData, float Pbr, int problematicTotal) {
        //找出最大的Upper
        HashMap<LinkedList<String>, Float> localScanPart = new HashMap<>();  //儲存計算的part，之後再PruningStrategy計算出要刪除的部分後，更新此表再重新計算Ugain數值找出最大
        findOrgPP.problematic.keySet().forEach((user) -> {
            HashSet<String> needCount = new HashSet<>();
            int trajectoryData_userSize = trajectoryData.get(user).size();
            float lostRate = 1.0f / (1.0f - ((trajectoryData_userSize - 1.0f) * (trajectoryData_userSize - 2.0f)) / (trajectoryData_userSize * (trajectoryData_userSize - 1.0f)));


            bipartiteData.trajectoryDataForPositionPart.get(user).forEach((part) -> needCount.addAll(bipartiteData.biT.get(part)));

            bipartiteData.trajectoryDataForPositionPart.get(user).forEach((part) -> part.forEach((location) -> {
                HashSet<String> subNeedCount = new HashSet<>(needCount);  //全部預計算的項目
                ArrayList<String> noNeedCount = new ArrayList<>(bipartiteData.trajectoryDataForPositionPart.keySet());

                LinkedList<String> findContainTjt = new LinkedList<>(part);  //移除軌跡找出關聯的t的bipartite分群項目
                findContainTjt.remove(location);

                //如果移除後有相關的子項目，將其加入
                if (bipartiteData.biT.containsKey(findContainTjt)) {
                    subNeedCount.addAll(bipartiteData.biT.get(findContainTjt));
                }

                //找出不需計算的項目進行
                noNeedCount.removeAll(subNeedCount);

                //算出upper計算的參數
                int upperLocationPart = noNeedCount.stream().mapToInt(t -> findOrgPP.problematic.getOrDefault(t, 0)).sum();

                float deleteRate = (float) (problematicTotal - upperLocationPart) / problematicTotal;

                float localUpper = deleteRate * lostRate;
//                System.out.println("User：" + user + "," + " Remove：" + location + ", " + noNeedCount + ", " + localUpper);
                LinkedList<String> localScan = new LinkedList<>();
                localScan.add(user);
                localScan.add(location);
                localScanPart.put(localScan, localUpper);
            }));
        });
//        System.out.println("**********************");
        Map.Entry<LinkedList<String>, Float> localMaxUpperPart = getMaxEntryInMapBasedOnValue(localScanPart);
//        System.out.println(localMaxUpperPart);
        //計算最大Upper中的實際Ugain數值
        float maxUpperForUgain = 0.0f;
        ArrayList<LinkedList<String>> singleUserBipartite = bipartiteData.trajectoryDataForPositionPart.get(localMaxUpperPart.getKey().get(0));
        for (LinkedList<String> part : singleUserBipartite) {
            if (part.contains(localMaxUpperPart.getKey().get(1))) {
                int trajectoryData_userSize = trajectoryData.get(localMaxUpperPart.getKey().get(0)).size();

                float lostRate = 1.0f / (1.0f - ((trajectoryData_userSize - 1.0f) * (trajectoryData_userSize - 2.0f)) / (trajectoryData_userSize * (trajectoryData_userSize - 1.0f)));
                //分母0值判斷
                if (Float.isNaN(lostRate)) {
                    lostRate = 1.0f;
                }

                bipartiteData.updateLocal_biT_biCT(part, bipartiteData.biT.get(part), localMaxUpperPart.getKey().get(1), localMaxUpperPart.getKey().get(0));
                FindPP findPPTotal = new FindPP(bipartiteData.new_biT, bipartiteData.new_biCT, Pbr, 1);  //二-1、掃描異常項目 check：0初始化/1loop計算
                float deleteRate = (float) (problematicTotal - findPPTotal.problematicTotal) / problematicTotal;
                maxUpperForUgain = deleteRate * lostRate;
            }
        }

        //刪除不可能具有最大項目的區域計算
        ArrayList<LinkedList<String>> updateLocalScanPart = new ArrayList<>();  //[[t8, b2], [t6, b1], [t7, a1], [t8, b3],.....]
//        for (LinkedList<String> part : localScanPart.keySet()) {
//            if (localScanPart.get(part) > maxUpperForUgain) {
//                updateLocalScanPart.add(part);
//            }
//        }
        float finalMaxUpperForUgain = maxUpperForUgain;
        localScanPart.forEach((part, value) -> {
            if (value > finalMaxUpperForUgain) {
                updateLocalScanPart.add(part);
            }
        });
        //離開區域程式計算
        float PS_effect = (float) updateLocalScanPart.size() / localScanPart.size();  //當PS_effect>指定數值，表示刪除率以過少，直接跳Global執行
        if (PS_effect > 0.9) {
            return null;

        } else if (updateLocalScanPart.isEmpty()) {
            ugainCount.addUgain();
            return localMaxUpperPart;

        }


        //重新計算剩餘的項目找出最大的upper
        HashMap<LinkedList<String>, Float> localUgainMap = new HashMap<>();
        for (LinkedList<String> localScan : updateLocalScanPart) {
            ArrayList<LinkedList<String>> moreUserBipartite = bipartiteData.trajectoryDataForPositionPart.get(localScan.get(0));

            for (LinkedList<String> part : moreUserBipartite) {
                if (part.contains(localScan.get(1))) {
                    ugainCount.addUgain();
                    LinkedList<String> localKey_and_Index = new LinkedList<>();
                    int trajectoryData_userSize = trajectoryData.get(localScan.get(0)).size();
                    float lostRate = 1.0f / (1.0f - ((trajectoryData_userSize - 1.0f) * (trajectoryData_userSize - 2.0f)) / (trajectoryData_userSize * (trajectoryData_userSize - 1.0f)));
                    //分母0值判斷
                    if (Float.isNaN(lostRate)) {
                        lostRate = 1.0f;
                    }

                    bipartiteData.updateLocal_biT_biCT(part, bipartiteData.biT.get(part), localScan.get(1), localScan.get(0));
                    FindPP findPPTotal = new FindPP(bipartiteData.new_biT, bipartiteData.new_biCT, Pbr, 1);  //二-1、掃描異常項目 check：0初始化/1loop計算
                    float deleteRate = (float) (problematicTotal - findPPTotal.problematicTotal) / problematicTotal;
                    float localUgain = deleteRate * lostRate;

                    //加入掃描的區域點的t以及位置
                    localKey_and_Index.add(localScan.get(0));
                    localKey_and_Index.add(localScan.get(1));
                    localUgainMap.put(localKey_and_Index, localUgain);
                }
            }
        }

        return getMaxEntryInMapBasedOnValue(localUgainMap);
    }

    /*
     * Find the entry with highest value
     * */
    private static <K, V extends Comparable<V>> Map.Entry<K, V> getMaxEntryInMapBasedOnValue(Map<K, V> map) {
        // To store the result
        Map.Entry<K, V> entryWithMaxValue = null;

        // Iterate in the map to find the required entry
        for (Map.Entry<K, V> currentEntry : map.entrySet()) {

            if (
                // If this is the first entry, set result as this
                    entryWithMaxValue == null

                            // If this entry's value is more than the max value
                            // Set this entry as the max
                            || currentEntry.getValue()
                            .compareTo(entryWithMaxValue.getValue())
                            > 0f) {

                entryWithMaxValue = currentEntry;
            }
        }

        // Return the entry with highest value
        return entryWithMaxValue;
    }

    /*
     * 計算 #trajectories
     * */
    protected static Integer TrajectoriesCount(HashMap<String, LinkedList<String>> trajectoryData) {
        int TrajectoriesCount = 0;
        //如果計算#trajectories的value不等於0，進行加總
        for (String strings : trajectoryData.keySet()) {
            if (trajectoryData.get(strings).size() != 0) {
                TrajectoriesCount += 1;

            }
        }

        return TrajectoriesCount;
    }

    /*
     * 計算 #locations
     * */
    protected static Integer locationsCount(HashMap<LinkedList<String>, ArrayList<String>> biT) {
        Iterator<LinkedList<String>> it = biT.keySet().iterator();
        int TrajectoriesCount = 0;

        while (it.hasNext()) {
            LinkedList<String> locationsKey = it.next();
            List<String> locationsValue = biT.get(locationsKey);
            TrajectoriesCount += locationsValue.size() * locationsKey.size();

        }

        return TrajectoriesCount;
    }
}

class UgainCount {
    protected int ugain_count = 0;

    protected void addUgain() {
        this.ugain_count++;
    }
}