package com.example.demo.controller.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.demo.controller.view.AdminRedeliveryViewController.SampleRedelivery;
import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.service.DriverService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// 再配達一覧・再配達詳細の画面のテスト（今は見本のデータ）
// TODO: A010・A013 につないだら、Service をモックにして「Service の結果を渡しているか」を確かめるテストに書き直す
@WebMvcTest(AdminRedeliveryViewController.class)
class AdminRedeliveryViewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DriverService driverService;

  @Test
  void 再配達一覧の画面に見本のデータと準備中の印を渡す() throws Exception {
    mockMvc.perform(get("/admin/redeliveries"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/redeliveries"))
        .andExpect(model().attribute("redeliveries", hasSize(3)))
        .andExpect(model().attribute("sampleData", true));// 画面に「準備中」の帯を出す
  }

  @Test
  void 再配達詳細の画面に見本のデータとドライバー一覧を渡す() throws Exception {
    List<DriverOptionResponse> drivers = List.of(new DriverOptionResponse(2L, "配送 一郎", "埼玉"));
    when(driverService.findDrivers()).thenReturn(drivers);

    MvcResult result = mockMvc.perform(get("/admin/redeliveries/1"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/redeliveryDetail"))
        .andExpect(model().attribute("drivers", drivers))
        .andExpect(model().attribute("sampleData", true))
        .andReturn();// andReturn()：結果を受け取って、あとで中身を確かめる

    // JSP に渡した再配達を取り出して、id=1 の見本のデータか確かめる
    // （hasProperty は getXxx() の形のメソッドを探すので、record（trackingNumber() の形）には使えない）
    SampleRedelivery redelivery = (SampleRedelivery) result.getModelAndView().getModel().get("redelivery");
    assertThat(redelivery.trackingNumber()).isEqualTo("100000000005");
  }

  @Test
  void 再配達詳細が見つからなければ404でエラー画面を出す() throws Exception {
    mockMvc.perform(get("/admin/redeliveries/99"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("common/error"))
        .andExpect(model().attribute("message", "該当する再配達が見つかりません"));
  }
}
