package com.gearwenxin.model.erniebot;

import com.gearwenxin.annotations.Between;
import com.gearwenxin.annotations.Only;
import com.gearwenxin.model.Message;
import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Ge Mingjia
 * @date 2023/7/20
 * <p>
 * ErnieBot 模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErnieSingleRoundRequest {

    /**
     * 表示最终用户的唯一标识符，可以监视和检测滥用行为，防止接口恶意调用
     */
    private String userId;

    /**
     * 聊天信息
     */
    @Size(max = 2000)
    @NotNull(message = "content is null !")
    private String content;

    /**
     * 输出更加随机，而较低的数值会使其更加集中和确定，默认0.95，范围 (0, 1.0]
     */
    @Only(value = 1)
    @Between(min = 0, max = 1.0, includeMax = true)
    private Float temperature;

    /**
     * （影响输出文本的多样性，越大生成文本的多样性越强
     */
    @Only(value = 1)
    @Between(min = 0, max = 1.0, includeMin = true, includeMax = true)
    private Float topP;

    /**
     * 通过对已生成的token增加惩罚，减少重复生成的现象。说明：
     */
    @Between(min = 1.0, max = 2.0, includeMin = true, includeMax = true)
    private Float penaltyScore;

    /**
     * 是否以流式接口的形式返回数据，默认false
     */
    private Boolean stream;

}
