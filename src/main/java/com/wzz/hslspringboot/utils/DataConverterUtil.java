package com.wzz.hslspringboot.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * @description: 将接口返回数据转换为目标接口所需数据结构的工具类
 * @author: YourName
 * @date: 2025-08-21
 */
public class DataConverterUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private DataConverterUtil() {
    }

    /**
     * 将源JSON数据转换为目标JSON数据结构
     *
     * @param sourceJson 源接口返回的JSON字符串
     * @return 转换后的目标JSON字符串
     */
    public static String convert(String sourceJson) {
        // 1. 使用Hutool将源JSON字符串解析为JSONObject
        JSONObject source = JSONUtil.parseObj(sourceJson);

        // 2. 创建一个新的JSONObject用于存放目标数据
        JSONObject target = new JSONObject();

        // 3. 开始进行字段映射和转换

        // --- 直接映射或简单类型转换的字段 ---
        target.set("zznm", source.getStr("zznm"));
        target.set("latitude", source.getDouble("latitude"));
        target.set("longitude", source.getDouble("longitude"));
        target.set("devicetype", source.getStr("devicetype"));
        target.set("yyfsmc", source.getStr("yyfsmc"));
        target.set("yypznm", source.getStr("yypznm"));
        target.set("zt", source.getStr("zt"));
        target.set("rq", source.getStr("rq"));
        target.set("yyfsnm", source.getStr("yyfsnm"));
        // double to int
        target.set("jl", source.getDouble("jl").intValue());

        // --- 需要根据业务逻辑调整的字段 ---
        // 在你的示例中，这些字段的值发生了变化，这里我们先按源数据进行映射。
        // 如果有固定规则，请在此处修改。
        // 例如，目标dz是"000000"，如果这是一个固定值或默认值，可以这样写：
        // target.set("dz", "000000");
        // 这里我们暂时从源数据映射，并添加注释
        target.set("dz", source.getStr("dz")); // TODO: 根据业务需求确认该字段的赋值逻辑
        target.set("fhsj", source.getStr("fhsj")); // TODO: 根据业务需求确认该字段的赋值逻辑
        target.set("lxfs", source.getStr("lxfs")); // TODO: 根据业务需求确认该字段的赋值逻辑
        target.set("zzmc", source.getStr("zzmc")); // TODO: 根据业务需求确认该字段的赋值逻辑

        // 在你的示例中，目标zsyhsl和xsfhsl的值均为200, yfhsl也为200。
        // 一个合理的推测是它们可能有关联。这里我们先从源数据获取。
        target.set("zsyhsl", source.getDouble("zsyhsl").intValue()); // TODO: 确认赋值逻辑
        target.set("xsfhsl", source.getDouble("xsfhsl").intValue()); // TODO: 确认赋值逻辑
        target.set("xssyhsl", source.getDouble("xssyhsl").intValue()); // TODO: 确认赋值逻辑


        // --- 复杂字段处理：lspz ---
        // 源lspz是一个JSON数组字符串，目标也一样，但内容不同。
        // 假设业务逻辑是只取源数据中的第一个粮食品种。
        String sourceLspzStr = source.getStr("lspz");
        if (JSONUtil.isJsonArray(sourceLspzStr)) {
            JSONArray sourceLspzArray = JSONUtil.parseArray(sourceLspzStr);
            if (!sourceLspzArray.isEmpty()) {
                // 取出第一个粮食品种对象
                JSONObject firstGrain = sourceLspzArray.getJSONObject(0);
                // 创建一个新的只包含第一个品种的JSONArray
                JSONArray targetLspzArray = new JSONArray();
                targetLspzArray.add(firstGrain);
                // 将新的JSONArray转为字符串存入target
                target.set("lspz", targetLspzArray.toString());
            }
        }

        // --- 复杂字段处理：yypzmxList ---
        JSONArray sourceYyList = source.getJSONArray("yypzmxList");
        JSONArray targetYyList = new JSONArray();
        if (sourceYyList != null && !sourceYyList.isEmpty()) {
            // 遍历源列表（这里只有一个元素，但代码按通用遍历来写）
            for (Object item : sourceYyList) {
                JSONObject sourceItem = (JSONObject) item;
                JSONObject targetItem = new JSONObject();

                // 映射yypzmxList内的字段
                targetItem.set("jssj", sourceItem.getStr("jssj"));
                targetItem.set("kssj", sourceItem.getStr("kssj"));
                targetItem.set("outTime", sourceItem.getBool("outTime"));
                // 暂时先从源数据映射
                targetItem.set("fhjssj", sourceItem.getStr("fhjssj"));
                targetItem.set("fhkssj", sourceItem.getStr("fhkssj"));
                targetItem.set("yypzmxnm", sourceItem.getStr("yypzmxnm"));

                // yfhsl在示例中是200，与zsyhsl/xsfhsl相同，这里做一个推测性映射
                // 假设yfhsl的值应与zsyhsl一致
                targetItem.set("yfhsl", target.getInt("zsyhsl"));

                targetYyList.add(targetItem);
            }
        }
        target.set("yypzmxList", targetYyList);

        // 4. 返回格式化后的目标JSON字符串
        return target.toStringPretty();
    }

    /**
     * main方法用于测试
     */
    public static void main(String[] args) {
        String sourceJson = "{\"jl\":115142.0,\"lspz\":\"[{\\\"nm\\\":\\\"200001\\\",\\\"name\\\":\\\"小麦\\\"},{\\\"nm\\\":\\\"200002\\\",\\\"name\\\":\\\"稻谷\\\"},{\\\"nm\\\":\\\"200003\\\",\\\"name\\\":\\\"玉米\\\"}]\",\"zznm\":\"zh8080816c611ae0016c6138cd030005O0000000000000005868\",\"latitude\":31.360919922358853,\"lxfs\":\"0553-5851663\",\"devicetype\":\"weixin\",\"zzmc\":\"中央储备粮芜湖直属库有限公司\",\"yyfsmc\":\"预约挂号\",\"yypznm\":\"a2d52d662ec34b3e912d70d08c24e5c6\",\"dz\":\"赤铸山西路10号中央储备粮芜湖直属库芜湖直属库有限公司\",\"fhsj\":\"05:00-06:00\",\"xssyhsl\":8.0,\"yypzmxList\":[{\"jssj\":\"2300\",\"fhjssj\":\"0600\",\"fhkssj\":\"0500\",\"kssj\":\"0500\",\"yfhsl\":30,\"yypzmxnm\":\"11ad7adad0df4e1db917fdc000fa3da7\",\"outTime\":false}],\"zt\":\"1\",\"xsfhsl\":15.0,\"zsyhsl\":30.0,\"longitude\":118.35924535989761,\"rq\":\"20250821\",\"yyfsnm\":\"1\"}";

        String targetJson = convert(sourceJson);

        System.out.println("--------- 转换后的JSON数据 ---------");
        System.out.println(targetJson);

        // 你可以再解析一次，验证结果是否符合预期
        JSONObject resultObj = JSONUtil.parseObj(targetJson);
        System.out.println("\n--------- 验证数据 ---------");
        System.out.println("粮食品种 (lspz): " + resultObj.getStr("lspz"));
        System.out.println("总收购数量 (zsyhsl): " + resultObj.getInt("zsyhsl"));
        System.out.println("yypzmxList中yfhsl: " + resultObj.getJSONArray("yypzmxList").getJSONObject(0).getInt("yfhsl"));
    }
}
