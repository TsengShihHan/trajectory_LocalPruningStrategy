package com.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Bipartite {
    public HashMap<String, LinkedList<String>> trajectoryData;  //原始軌跡資料
    public HashMap<String, ArrayList<LinkedList<String>>> trajectoryDataForPositionPart;  //每條軌跡資料被分配到的bipartite項目
    public HashMap<LinkedList<String>, ArrayList<String>> biT = new HashMap<>();  //scan建立的Bipartite
    public HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> biCT = new HashMap<>();  //Bipartite對應投影的關聯
    public HashMap<LinkedList<String>, ArrayList<String>> new_biT;
    public HashMap<LinkedList<String>, ArrayList<LinkedList<String>>> new_biCT;
    public LinkedList<String> unifying;

    public Bipartite(HashMap<String, LinkedList<String>> trajectoryData) {
        this.trajectoryData = trajectoryData;
        this.trajectoryDataForPositionPart = new HashMap<>();  //EX：t4=[[a2, a3], [b1]], t5=[[a3, a1], [b1]].....
        build();
    }

    /*
     * 建立Bipartite & 關聯
     * */
    public void build() {
        this.trajectoryData.forEach((someOne, a_Trajectory) -> {
            LinkedList<String> aLocation = new LinkedList<>();
            LinkedList<String> bLocation = new LinkedList<>();
            ArrayList<LinkedList<String>> part = new ArrayList<>();  //暫存紀錄每個t會有的組合
            ArrayList<String> personTA = new ArrayList<>();  //紀錄軌跡符合的人
            ArrayList<String> personTB = new ArrayList<>();  //紀錄軌跡符合的人
            ArrayList<LinkedList<String>> AC = new ArrayList<>();  //A的關聯
            ArrayList<LinkedList<String>> BC = new ArrayList<>();  //B的關聯

            //每個位置掃描
            a_Trajectory.forEach((location) -> {
                //判斷字首位置
                if (location.startsWith("a")) {
                    aLocation.add(location);
                } else {
                    bLocation.add(location);
                }
            });

            if (aLocation.size() != 0) {
                if (biT.containsKey(aLocation)) {
                    personTA = biT.get(aLocation);
                }
                personTA.add(someOne);
                part.add(aLocation);
                biT.put(aLocation, personTA);
            }

            if (bLocation.size() != 0) {
                if (biT.containsKey(bLocation)) {
                    personTB = biT.get(bLocation);
                }
                personTB.add(someOne);
                part.add(bLocation);
                biT.put(bLocation, personTB);
            }
            trajectoryDataForPositionPart.put(someOne, part);  //加入區域專用的valut對應key值項目

            //add edge
            if (aLocation.size() != 0 && bLocation.size() != 0) {
                //建立a關聯
                if (this.biCT.containsKey(aLocation)) {
                    AC = this.biCT.get(aLocation);
                }
                if (!AC.contains(bLocation)) {
                    AC.add(bLocation);
                    this.biCT.put(aLocation, AC);
                }

                //建立b關聯
                if (this.biCT.containsKey(bLocation)) {
                    BC = this.biCT.get(bLocation);
                }
                if (!BC.contains(aLocation)) {
                    BC.add(aLocation);
                    this.biCT.put(bLocation, BC);
                }
            }
        });
    }

    /*
     * 更新重建biT、biCT
     * */
    public void update_biT_biCT(LinkedList<LinkedList<String>> checkPart) {
        this.new_biCT = new HashMap<>(this.biCT);
        this.new_biT = new HashMap<>(this.biT);
        this.unifying = new LinkedList<>(checkPart.get(0));
        LinkedList<String> with = new LinkedList<>(checkPart.get(1));

        if (!with.isEmpty()) {
            this.unifying.removeAll(with);  //取出差集項目，移除相同值，剩餘值為欲從原始軌跡中移除的項目
            ArrayList<String> tem_biT_unifyingList = new ArrayList<>(this.biT.get(checkPart.get(0))); //取出比較軌跡路徑包含的t(某個人) ex.[t5, t6, t7]
            ArrayList<LinkedList<String>> tem_biCT_unifyingList = new ArrayList<>(this.biCT.getOrDefault(checkPart.get(0), new ArrayList<>()));  //取出比較軌跡路徑所對應的投影位置 if[a3, a1]，對應是 [[b1], [b2]]
            ArrayList<String> tem_biT_value = new ArrayList<>(this.biT.get(checkPart.get(1)));
            ArrayList<LinkedList<String>> tem_biCT_value = new ArrayList<>(this.biCT.getOrDefault(checkPart.get(1), new ArrayList<>()));  //取出比較軌跡路徑所對應的投影位置

            //移除愈刪除的關聯項目
            this.new_biT.remove(checkPart.get(0));
            this.new_biCT.remove(checkPart.get(0));
            //加入移除的value項目給新的
            tem_biT_value.addAll(tem_biT_unifyingList);
            //edge更新
            if (!tem_biCT_unifyingList.isEmpty()) {
                tem_biCT_unifyingList.forEach((biCT_Item) -> {
                    if (!tem_biCT_value.contains(biCT_Item)) {
                        tem_biCT_value.add(biCT_Item);
                    }
                });
                this.new_biCT.put(checkPart.get(1), tem_biCT_value);

                //更新對應的連結
                tem_biCT_unifyingList.forEach((CT_part) -> {
                    ArrayList<LinkedList<String>> a = new ArrayList<>(new_biCT.get(CT_part));  //a是暫存變數命名....不知道要命甚麼，取得關聯投影過去的位置// (if[a3, a1]，對應是 [[b1], [b2]]，要把b1、b2有連給[a3, a1]的項目移除)
                    a.remove(checkPart.get(0));
                    //有包含項目的話就跳過
                    if (!a.contains(checkPart.get(1))) {
                        a.add(checkPart.get(1));
                    }

                    this.new_biCT.put(CT_part, a);

                });
            }


            //更新進去
            this.new_biT.put(checkPart.get(1), tem_biT_value);


        } else {
            ArrayList<LinkedList<String>> tem_biCT_unifyingList = new ArrayList<>(this.biCT.getOrDefault(checkPart.get(0), new ArrayList<>()));  //取出比較軌跡路徑所對應的投影位置

            this.new_biT.remove(checkPart.get(0));

            if (!tem_biCT_unifyingList.isEmpty()) {
                this.new_biCT.remove(checkPart.get(0));

                //更新對應的連結
                tem_biCT_unifyingList.forEach((CT_part) -> {
                    ArrayList<LinkedList<String>> a = new ArrayList<>(new_biCT.get(CT_part));
                    a.remove(checkPart.get(0));
                    this.new_biCT.put(CT_part, a);

                });
            }
        }
    }

    /*
     * 更新local變化的biT、biCT
     * */
    public void updateLocal_biT_biCT(LinkedList<String> a_tjtPart, ArrayList<String> userList, String deleteLocalPoint, String user) {
        this.new_biCT = new HashMap<>(this.biCT);
        this.new_biT = new HashMap<>(this.biT);

        LinkedList<String> updateSourcePart = new LinkedList<>(a_tjtPart);
        ArrayList<String> updateOrgSourceUserList = new ArrayList<>(userList);
        LinkedList<String> targetPart = findCorSide(this.biCT.get(a_tjtPart), user);  //一個user根據bipartite結構找出user另一端的key軌跡

        updateSourcePart.remove(deleteLocalPoint);  //區域更新軌跡節點路徑
        updateOrgSourceUserList.remove(user);  //路徑更新後要刪除原本路徑包含的user

        if (!updateOrgSourceUserList.isEmpty()) {
            this.new_biT.put(a_tjtPart, updateOrgSourceUserList);  //跟新刪除的
            //如果原使軌跡節點以是單一項目無關連，也不必進行更新edge了
            if (!targetPart.isEmpty()) {
                //開始檢查edge關聯是否保留或更新，與目標user進行交集運算，如果為空值，進行更新刪除
                ArrayList<String> checkEdgeUpdate = new ArrayList<>(updateOrgSourceUserList);
                checkEdgeUpdate.retainAll(this.biT.get(targetPart));

                if (checkEdgeUpdate.isEmpty()) {
                    ArrayList<LinkedList<String>> edgeUpdate1 = new ArrayList<>(this.new_biCT.get(a_tjtPart));  //雙向邊緣更新1

                    if (edgeUpdate1.size() == 1) {
                        this.new_biCT.remove(a_tjtPart);

                    } else {
                        edgeUpdate1.remove(targetPart);
                        this.new_biCT.put(a_tjtPart, edgeUpdate1);

                    }

                    ArrayList<LinkedList<String>> edgeUpdate2 = new ArrayList<>(this.new_biCT.get(targetPart));  //雙向邊緣更新2

                    if (edgeUpdate2.size() == 1) {
                        this.new_biCT.remove(targetPart);

                    } else {
                        edgeUpdate2.remove(a_tjtPart);
                        this.new_biCT.put(targetPart, edgeUpdate2);

                    }
                }
            }

        } else {
            this.new_biT.remove(a_tjtPart);  //刪除軌跡節點
            this.new_biCT.remove(a_tjtPart);  //刪除關聯軌跡
            //如果之後執行這邊有錯，應該是要加入isEmpty判斷
            if (this.new_biCT.containsKey(targetPart)) {
                ArrayList<LinkedList<String>> edgeUpdate3 = new ArrayList<>(this.new_biCT.get(targetPart));  //雙向邊緣更新3
                edgeUpdate3.remove(a_tjtPart);

                if (edgeUpdate3.isEmpty()) {
                    this.new_biCT.remove(targetPart);

                } else {
                    this.new_biCT.put(targetPart, edgeUpdate3);

                }
            }


        }
        //更新後的軌跡節點來源不為空值，才可進行檢查
        if (!updateSourcePart.isEmpty()) {
            ArrayList<String> sourceUpdateUserList = new ArrayList<>();  //取得更新路徑軌跡在原始結構中包含的user，若該更新路徑軌跡不存在原本的結構，則表示為新的路徑節點
            //檢查bipartite是否有包含更新過的來源軌跡節點
            if (this.new_biT.containsKey(updateSourcePart)) {
                sourceUpdateUserList.addAll(this.new_biT.get(updateSourcePart));

            }
            sourceUpdateUserList.add(user);
            this.new_biT.put(updateSourcePart, sourceUpdateUserList);

            //如果原使軌跡節點以是單一項目無關連，也不必進行更新edge了
            if (!targetPart.isEmpty()) {
                ArrayList<LinkedList<String>> edgeUpdate4 = new ArrayList<>();
                if (this.new_biCT.containsKey(updateSourcePart)) {
                    edgeUpdate4.addAll(this.new_biCT.get(updateSourcePart));  //雙向邊緣更新4
                }
                edgeUpdate4.add(targetPart);
                this.new_biCT.put(updateSourcePart, edgeUpdate4);

                ArrayList<LinkedList<String>> edgeUpdate5 = new ArrayList<>();
                if (this.new_biCT.containsKey(targetPart)){
                    edgeUpdate5.addAll(this.new_biCT.get(targetPart));
                }
                edgeUpdate5.add(updateSourcePart);
                this.new_biCT.put(targetPart, edgeUpdate5);
            }

        }

    }

    /*
     * 更新重建biT、biCT
     * */
    public void update_unifying(LinkedList<LinkedList<String>> checkPart) {
        this.unifying = new LinkedList<>(checkPart.get(0));
        LinkedList<String> with = new LinkedList<>(checkPart.get(1));

        if (!with.isEmpty()) {
            this.unifying.removeAll(with);  //取出差集項目，移除相同值，剩餘值為欲從原始軌跡中移除的項目

        }
    }

    /*
     * 找出區域bipartite的邊，所對應的另一組邊
     * */
    private LinkedList<String> findCorSide(ArrayList<LinkedList<String>> biCT_list, String user) {
        LinkedList<String> returnKey = new LinkedList<>();

        if (biCT_list != null) {
            biCT_list.forEach((key) -> {
                if (this.biT.get(key).contains(user)) {
                    returnKey.addAll(key);
                }
            });
        }

        return returnKey;
    }
}
