package com.company;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FindPP {
    private HashMap<String, ArrayList<String>> CPt_map;  //關聯的異常Pt數值儲存暫存表
    public HashSet<LinkedList<String>> problematicDenominatorKey = new HashSet<>();  //紀錄異常項目(分母值)
    public HashMap<String, Integer> problematic = new HashMap<>();  //異常t的總表
    public Integer problematicTotal = 0;

    public FindPP(HashMap<LinkedList<String>, ArrayList<String>> biT, HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> biCT, float Pbr, Integer check) {
        switch (check) {
            //0為初始化跑的計算
            case 0 :
                initialization(biT, biCT, Pbr);
                break;
            //1為不斷重複計算的函數
            case 1 :
                loopCalculation(biT, biCT, Pbr);
                break;

        }
    }

    public FindPP(HashMap<String, Integer> problematic, HashSet<String> chang_t, HashMap<LinkedList<String>, ArrayList<String>> biT, HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> biCT, float Pbr) {
        biT.forEach((tEdgeNode, tValue) -> {
            this.CPt_map = new HashMap<>();

            if (biCT.get(tEdgeNode) != null) {
                biCT.get(tEdgeNode).forEach((ctEdgeNode) -> {
                    ArrayList<String> denominatorT_Value = new ArrayList<>(tValue);  //取得分母位置包含的t
                    denominatorT_Value.retainAll(biT.get(ctEdgeNode));  //取2位置所交集的t

                    ctEdgeNode.forEach((a_ctEdgeNode) -> {
                        ArrayList<String> ctValueItem = this.CPt_map.getOrDefault(a_ctEdgeNode, new ArrayList<>());  //取出相同的key軌跡位置，將某個人(t的值放進去)
                        ctValueItem.addAll(denominatorT_Value);
                        this.CPt_map.put(a_ctEdgeNode, ctValueItem);
                    });
                });
            }
            //將相關的項目進行掃苗
            CPt_map.forEach((a_ctEdgeNode, ctValueItem) -> {
                //判斷計算大小
                if (((float) ctValueItem.size() / tValue.size()) > Pbr) {
                    //將有問題的t加入異常次數計算
                    ctValueItem.forEach((someonePartData) -> {
                        if (chang_t.contains(someonePartData)) {
                            this.problematicTotal += 1;
                        }
                    });
                }
            });
        });

        for (String tPart : problematic.keySet()) {
            if (!chang_t.contains(tPart)) {
                this.problematicTotal += problematic.get(tPart);
            }
        }
    }

    /*
     * 0：初始化計算
     * */
    public void initialization(HashMap<LinkedList<String>, ArrayList<String>> biT, HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> biCT, float Pbr) {
        biT.forEach((tEdgeNode, tValue) -> {
            this.CPt_map = new HashMap<>();

            if (biCT.get(tEdgeNode) != null) {
                biCT.get(tEdgeNode).forEach((ctEdgeNode) -> {
                    ArrayList<String> denominatorT_Value = new ArrayList<>(tValue);  //取得分母位置包含的t

                    denominatorT_Value.retainAll(biT.get(ctEdgeNode));  //取2位置所交集的t

                    ctEdgeNode.forEach((a_ctEdgeNode) -> {
                        ArrayList<String> ctValueItem = this.CPt_map.getOrDefault(a_ctEdgeNode, new ArrayList<>());  //取出相同的key軌跡位置，將某個人(t的值放進去)
                        ctValueItem.addAll(denominatorT_Value);
                        this.CPt_map.put(a_ctEdgeNode, ctValueItem);
                    });
                });
            }


            //將相關的項目進行掃苗
            CPt_map.forEach((a_ctEdgeNode, ctValueItem) -> {
                //判斷計算大小
                if (((float) ctValueItem.size() / tValue.size()) > Pbr) {
                    this.problematicDenominatorKey.add(tEdgeNode);  //加入異常標記
                    //將有問題的t加入異常次數計算
                    for (String t : ctValueItem) {
                        Integer count = this.problematic.getOrDefault(t, 0);  //取得異常表的t異常總數，若無則預設0
                        count += 1;  //將該t+1更新後，put更新總表
                        this.problematicTotal += 1;
                        this.problematic.put(t, count);
                    }
                }
            });
        });
    }

    /*
     * 1：不斷重複計算
     * */
    public void loopCalculation(HashMap<LinkedList<String>, ArrayList<String>> biT, HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> biCT, float Pbr) {
        biT.forEach((tEdgeNode, tValue) -> {
            this.CPt_map = new HashMap<>();

            if (biCT.get(tEdgeNode) != null) {
                biCT.get(tEdgeNode).forEach((ctEdgeNode) -> {
                    ArrayList<String> denominatorT_Value = new ArrayList<>(tValue);  //取得分母位置包含的t
                    denominatorT_Value.retainAll(biT.get(ctEdgeNode));  //取2位置所交集的t

                    ctEdgeNode.forEach((a_ctEdgeNode) -> {
                        ArrayList<String> ctValueItem = this.CPt_map.getOrDefault(a_ctEdgeNode, new ArrayList<>());  //取出相同的key軌跡位置，將某個人(t的值放進去)
                        ctValueItem.addAll(denominatorT_Value);
                        this.CPt_map.put(a_ctEdgeNode, ctValueItem);
                    });
                });
            }
            //將相關的項目進行掃苗
            CPt_map.forEach((a_ctEdgeNode, ctValueItem) -> {
                //判斷計算大小
                if (((float) ctValueItem.size() / tValue.size()) > Pbr) {
                    //將有問題的t加入異常次數計算
                    ctValueItem.forEach((someonePartData) -> this.problematicTotal += 1);
                }
            });
        });
    }

    /*
     * 組合出待檢查的項目(不包含空集合)
     * */
    public ArrayList<LinkedList<LinkedList<String>>> combinationCheckWithoutEmpty(HashSet<LinkedList<String>> problematicDenominatorKey, Set<LinkedList<String>> biT_keySet) {
        ArrayList<LinkedList<LinkedList<String>>> checkPart = new ArrayList<>();  //儲存待檢查項目

        biT_keySet.forEach((biT_Key_L) -> biT_keySet.forEach((biT_Key_R) -> {
            AtomicBoolean check = new AtomicBoolean(true);

            //如果不相同，以及標記軌跡有包含在原始軌跡中，組成一組合
            if (!biT_Key_L.equals(biT_Key_R) && biT_Key_L.size() > biT_Key_R.size()) {
                int index = 0;

                for (String problematicKey_Location : biT_Key_R) {
                    if (biT_Key_L.indexOf(problematicKey_Location) >= index) {
                        index = biT_Key_L.indexOf(problematicKey_Location);
                    } else {
                        check.set(false);
                        break;
                    }
                }
                if (check.get() && (problematicDenominatorKey.contains(biT_Key_L) || problematicDenominatorKey.contains(biT_Key_R))) {
                    LinkedList<LinkedList<String>> checkPartData = new LinkedList<>();
                    checkPartData.add(biT_Key_L);
                    checkPartData.add(biT_Key_R);
                    checkPart.add(checkPartData);
                }
            }
        }));

        return checkPart;
    }

    /*
     * 組合出待檢查的項目(含空集合)
     * */
    public ArrayList<LinkedList<LinkedList<String>>> combinationCheckWithEmpty(HashSet<LinkedList<String>> problematicDenominatorKey, Set<LinkedList<String>> biT_keySet) {
        ArrayList<LinkedList<LinkedList<String>>> checkPart = new ArrayList<>();  //儲存待檢查項目

        biT_keySet.forEach((biT_Key_L) -> biT_keySet.forEach((biT_Key_R) -> {
            AtomicBoolean check = new AtomicBoolean(true);

            //如果不相同，以及標記軌跡有包含在原始軌跡中，組成一組合
            if (!biT_Key_L.equals(biT_Key_R) && biT_Key_L.size() > biT_Key_R.size()) {
                int index = 0;

                for (String problematicKey_Location : biT_Key_R) {
                    if (biT_Key_L.indexOf(problematicKey_Location) >= index) {
                        index = biT_Key_L.indexOf(problematicKey_Location);
                    } else {
                        check.set(false);
                        break;
                    }
                }
                if (check.get() && (problematicDenominatorKey.contains(biT_Key_L) || problematicDenominatorKey.contains(biT_Key_R))) {
                    LinkedList<LinkedList<String>> checkPartData = new LinkedList<>();
                    checkPartData.add(biT_Key_L);
                    checkPartData.add(biT_Key_R);
                    checkPart.add(checkPartData);
                }
            }
        }));

        //加入空集合
        problematicDenominatorKey.forEach((problematicKey) -> {
            LinkedList<LinkedList<String>> checkPartData = new LinkedList<>();
            checkPartData.add(problematicKey);
            checkPartData.add(new LinkedList<>());
            checkPart.add(checkPartData);
        });
        return checkPart;
    }
}
