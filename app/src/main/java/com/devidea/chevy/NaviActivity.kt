package com.devidea.chevy

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.databinding.ActivityNaviBinding
import com.kakao.sdk.v2.common.BuildConfig.VERSION_NAME
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafetyType
import com.kakaomobility.knsdk.ui.view.KNNaviView

class NaviActivity : AppCompatActivity(), KNGuidance_SafetyGuideDelegate {

    lateinit var binding : ActivityNaviBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        KNSDK.sharedGuidance()?.apply {
            binding.naviView.initWithGuidance(this, null, KNRoutePriority.KNRoutePriority_Recommand, KNRouteAvoidOption.KNRouteAvoidOption_None.value)
        }

    }

    // 주변의 안전 운행 정보가 업데이트될 때 호출됩니다.
    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        // 업데이트된 주변 안전 정보 리스트가 null이 아닌 경우
        safeties?.let {
            for (safety in it) {
                // 예: 각 안전 정보의 타입과 설명을 로그로 출력
                Log.d("AroundSafety", "Safety type: ${safety.safetyType()}, description: ${safety.isOnStraightWay()}")

                // 예: UI에 각 주변 안전 정보를 표시하는 코드 추가 가능
                addSafetyToUI(safety)
            }
        }
    }

    // 주변 안전 정보를 UI에 추가하는 메서드 (예시)
    fun addSafetyToUI(safety: KNSafety) {
        when(safety.safetyType()){
            KNSafetyType.KNSafetyType_Caution -> {

            }

            KNSafetyType.KNSafetyType_Camera -> {
                Log.d("AroundSafety", "Safety location: ${safety.location}, code: ${safety.code}")
                safety.location
            }

            KNSafetyType.KNSafetyType_Section -> {

            }
        }
    }

    // 안전 운행 정보가 업데이트될 때 호출됩니다.
    override fun guidanceDidUpdateSafetyGuide(guidance: KNGuidance, safetyGuide: KNGuide_Safety?) {
        // 안전 운행 정보가 null이 아닌 경우
        safetyGuide?.let {
            // 예: 안전 운행 정보의 설명을 로그로 출력
            Log.d("SafetyGuide", "Updated safety information: ${it.safetiesOnGuide}")

            // 예: UI에 안전 운행 정보를 표시하는 코드 추가 가능
            showSafetyGuideOnUI(it)
        }
    }

    // 안전 운행 정보를 UI에 표시하는 메서드 (예시)
    fun showSafetyGuideOnUI(safetyGuide: KNGuide_Safety) {

    }
}