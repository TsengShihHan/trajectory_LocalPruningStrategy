package com.company;

import java.util.*;

public class Global_sup extends Main {
    public Global_sup(HashMap<String, LinkedList<String>> trajectoryData, float Pbr, long createBipartiteGraphStartTime, long createBipartiteGraphEndTime, String inputFileName, int ugain_count) {
        int checkPartPoint = 0;  //判定checkPartList是否為空變數，若是空的則=1，之後比對加入空集合
        int gain = ugain_count;  //計算gain次數(待check檢查總次數)
        ArrayList<LinkedList<LinkedList<String>>> checkPartList;

        Bipartite bipartiteData = new Bipartite(trajectoryData);  //一、取得建立bipartiteData的分群資料以及投影關聯

        while (true) {
            FindPP findOrgPP = new FindPP(bipartiteData.biT, bipartiteData.biCT, Pbr, 0);  //二-1、掃描異常項目 check：0初始化/1loop計算

            if (findOrgPP.problematicTotal.equals(0)) {
                break;

            }  //終止條件設定

            if (checkPartPoint == 0) {
                checkPartList = findOrgPP.combinationCheckWithoutEmpty(findOrgPP.problematicDenominatorKey, bipartiteData.biT.keySet());  //二-2、組合代檢查項目

                if (checkPartList.size() == 0) {
                    checkPartPoint = 1;
                    checkPartList = findOrgPP.combinationCheckWithEmpty(findOrgPP.problematicDenominatorKey, bipartiteData.biT.keySet());  //二-2、組合代檢查項目

                }
            } else {
                checkPartList = findOrgPP.combinationCheckWithEmpty(findOrgPP.problematicDenominatorKey, bipartiteData.biT.keySet());  //二-2、組合代檢查項目

            }
//            gain += checkPartList.size();  //計算gain次數(待check檢查總次數)

            gain += findMaxUgainPart(checkPartList, trajectoryData, bipartiteData, findOrgPP, Pbr);  //三、掃描全部組合找出最大Ugain項目，並且更新原始表格

            bipartiteData = new Bipartite(trajectoryData);  //一、取得建立bipartiteData的分群資料以及投影關聯
        }

        long programEndTime = System.currentTimeMillis();   //獲取程式最結束終時間

        System.out.println("1.建立bipartite graph的時間：" + (createBipartiteGraphEndTime - createBipartiteGraphStartTime) + "ms");
        System.out.println("2.建完bipartite graph後，一直到最終結束所花的時間：" + (programEndTime - createBipartiteGraphEndTime) + "ms");
        System.out.println("3.計算gain的總次數：" + gain + "次");
        System.out.println("4.最後有多少 #trajectories：\t" + TrajectoriesCount(trajectoryData) + "個；" + trajectoryData);
        System.out.println("5.原始 #trajectories：\t\t" + TrajectoriesCount(getTrajectoryData(inputFileName)) + "個；");
        System.out.println("6.最後有多少 #locations：\t" + locationsCount(bipartiteData.biT) + "個；");
        System.out.println("7.原始 #locations：\t\t\t" + locationsCount(new Bipartite(getTrajectoryData(inputFileName)).biT) + "個；");

    }

    /*
     * 掃描待檢查組合項目找出最大Ugain數值
     * */
    private static int findMaxUgainPart(ArrayList<LinkedList<LinkedList<String>>> checkPartList, HashMap<String, LinkedList<String>> trajectoryData, Bipartite bipartiteData, FindPP findOrgPP, float Pbr) {
        HashMap<LinkedList<LinkedList<String>>, HashSet<String>> chang_t = new HashMap<>();  //每個檢查點需計算的t項目
        HashMap<LinkedList<LinkedList<String>>, Float> checkPart_upperMap = new HashMap<>();  //每個檢查點的upper的數值
        ArrayList<LinkedList<LinkedList<String>>> max_upperCheckPart = new ArrayList<>();  //紀錄擁有最大upper的CheckPart組合，由於可能會有一樣的，因此使用ArrayList儲存
        LinkedList<LinkedList<String>> maxCheckPart = new LinkedList<>();

        float max_upper = (float) -1.0;
        float max_upperForUgain = (float) -1.0;  //找出最大upper組合中，最大的Ugain數值
        float maxU_gain = (float) -1.0;
        int ugin;

        // 掃描所有關聯
        checkPartList.forEach((checkPart) -> {
            HashSet<String> tmp_chang_t = new HashSet<>();  //暫存 PruningStrategy1不須計算的t項目
            checkPart.forEach((checkPartKey) -> {
                if (!checkPartKey.isEmpty()) {
                    tmp_chang_t.addAll(bipartiteData.biT.get(checkPartKey));
                    //第二層位置所對應到的關聯位置包含的t
                    if (bipartiteData.biCT.containsKey(checkPartKey)) {
                        bipartiteData.biCT.get(checkPartKey).forEach((correspondingCheckPart) -> tmp_chang_t.addAll(bipartiteData.biT.get(correspondingCheckPart)));
                    }
                }
            });

            chang_t.put(checkPart, tmp_chang_t);
        });

        // 計算upper，取出最大的checkPart組合，以及紀錄全部checkPart的upper數值
        for (LinkedList<LinkedList<String>> checkPart : checkPartList) {
            int PS2_problematic = 0;  //紀錄不須計算的加總
            float DenominatorData = (float) 0;  //公式中分母的計算累加值
            float upper;

            bipartiteData.update_unifying(checkPart);  //找出刪除的個數

            //計算PS2_problematic總數
            for (Map.Entry<String, Integer> entry : findOrgPP.problematic.entrySet()) {
                String t = entry.getKey();
                Integer t_problematicCount = entry.getValue();
                //如果t不是在需計算的項目，就是不必計算的項目，因此更新加總
                if (!chang_t.get(checkPart).contains(t)) {
                    PS2_problematic += t_problematicCount;
                }
            }

            //計算upper數值
            for (String T_unifyingPerson_t : bipartiteData.biT.get(checkPart.get(0))) {
                float denominatorMolecular = trajectoryData.get(T_unifyingPerson_t).size() - bipartiteData.unifying.size();  //公式中分母裡面的分子項目，為比較項目中t所包含的軌跡項目數量 扣除 移除的軌跡數
                float denominatorDenominator = trajectoryData.get(T_unifyingPerson_t).size();
                DenominatorData += (1.0f - ((denominatorMolecular * (denominatorMolecular - 1.0f)) / (denominatorDenominator * (denominatorDenominator - 1.0f))));
                if (Float.isNaN(DenominatorData)) {
                    DenominatorData = 1.0f;
                }
//                System.out.println("測試：" + 0.0f + (1.0f - ((0.0f * (0.0f - 1.0f)) / (1.0f * (1.0f - 1.0f)))));
            }
//            System.out.println("(" + "(" + findOrgPP.problematicTotal + "-" + PS2_problematic + ")" + "/" + findOrgPP.problematicTotal + ") * (1/" + DenominatorData + ")");
            upper = ((float) (findOrgPP.problematicTotal - PS2_problematic) / findOrgPP.problematicTotal) * ((float) 1 / DenominatorData);
            checkPart_upperMap.put(checkPart, upper);

            if (max_upper < upper) {
                max_upper = upper;
                max_upperCheckPart.clear();
                max_upperCheckPart.add(checkPart);

            } else if (max_upper == upper) {
                max_upperCheckPart.add(checkPart);

            }

        }

        // 計算最大upper組合中的ugain，找出最大的ugain
        for (LinkedList<LinkedList<String>> checkPart : max_upperCheckPart) {
            float DenominatorData = (float) 0;  //公式中分母的計算累加值
            float U_gain;

            bipartiteData.update_biT_biCT(checkPart);  //將每個愈計算的checkPart進行計算，重建bipartite以及關聯表
            FindPP findLoopPP = new FindPP(findOrgPP.problematic, chang_t.get(checkPart), bipartiteData.new_biT, bipartiteData.new_biCT, Pbr);  //二-1、掃描異常項目 加入PruningStrategy1 的計算

            for (String T_unifyingPerson_t : bipartiteData.biT.get(checkPart.get(0))) {
                float denominatorMolecular = trajectoryData.get(T_unifyingPerson_t).size() - bipartiteData.unifying.size();  //公式中分母裡面的分子項目，為比較項目中t所包含的軌跡項目數量 扣除 移除的軌跡數
                float denominatorDenominator = trajectoryData.get(T_unifyingPerson_t).size();
                DenominatorData += (1 - ((denominatorMolecular * (denominatorMolecular - 1)) / (denominatorDenominator * (denominatorDenominator - 1))));
            }
            U_gain = ((float) (findOrgPP.problematicTotal - findLoopPP.problematicTotal) / findOrgPP.problematicTotal) * ((float) 1 / DenominatorData);

            if (maxU_gain < U_gain) {
                max_upperForUgain = U_gain;

            }
        }

        // 刪除小於"上面步驟找出ugain"的upper數值組合
        ArrayList<LinkedList<LinkedList<String>>> checkPart_upperMap_KeySet = new ArrayList<>();
        for (LinkedList<LinkedList<String>> checkPart_upperMap_Key : checkPart_upperMap.keySet()) {
            if (checkPart_upperMap.get(checkPart_upperMap_Key) >= max_upperForUgain) {
                checkPart_upperMap_KeySet.add(checkPart_upperMap_Key);
            }
        }

        ugin = checkPart_upperMap_KeySet.size();  //計算真正刪除後的Ugain數量
        // 重新計算找出真正最大的ugain組合
        for (LinkedList<LinkedList<String>> checkPart : checkPart_upperMap_KeySet) {
            float DenominatorData = (float) 0;  //公式中分母的計算累加值
            float U_gain;

            bipartiteData.update_biT_biCT(checkPart);  //將每個愈計算的checkPart進行計算，重建bipartite以及關聯表
            FindPP findLoopPP = new FindPP(findOrgPP.problematic, chang_t.get(checkPart), bipartiteData.new_biT, bipartiteData.new_biCT, Pbr);  //二-1、掃描異常項目 加入PruningStrategy1 的計算

            for (String T_unifyingPerson_t : bipartiteData.biT.get(checkPart.get(0))) {
                float denominatorMolecular = trajectoryData.get(T_unifyingPerson_t).size() - bipartiteData.unifying.size();  //公式中分母裡面的分子項目，為比較項目中t所包含的軌跡項目數量 扣除 移除的軌跡數
                float denominatorDenominator = trajectoryData.get(T_unifyingPerson_t).size();
                DenominatorData += (1 - ((denominatorMolecular * (denominatorMolecular - 1)) / (denominatorDenominator * (denominatorDenominator - 1))));
                if (Float.isNaN(DenominatorData)) {
                    DenominatorData = 1.0f;
                }
            }
            U_gain = ((float) (findOrgPP.problematicTotal - findLoopPP.problematicTotal) / findOrgPP.problematicTotal) * ((float) 1 / DenominatorData);

            if (maxU_gain < U_gain) {
                maxU_gain = U_gain;
                maxCheckPart = checkPart;
            }
        }

        bipartiteData.unifying = new LinkedList<>(maxCheckPart.get(0));
        bipartiteData.unifying.removeAll(maxCheckPart.get(1));
        //更新原始軌跡表 (若看不懂，參見v2 到最後才加入空集合 + 不跟原先的計算項目數值做檢查是否包含的版本 中的456行)
        bipartiteData.biT.get(maxCheckPart.get(0)).forEach((updateTrajectoriesTaleKey) -> bipartiteData.unifying.forEach((removeItem) -> trajectoryData.get(updateTrajectoriesTaleKey).removeIf(s -> s.equals(removeItem))));

        return ugin;
    }
}
