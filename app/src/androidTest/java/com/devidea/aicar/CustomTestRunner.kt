package com.devidea.aicar

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Hilt 테스트를 위한 커스텀 러너

/**
 * java.lang.IllegalStateException: Hilt test, com.devidea.aicar.DrivingDaoTest, cannot use a @HiltAndroidApp application but found com.devidea.aicar.App. To fix, configure the test to use HiltTestApplication or a custom Hilt test application generated with @CustomTestApplication.
 * Hilt 테스트가 실제 프로덕션용 Application 클래스(com.devidea.aicar.App)를 사용하려고 해서 발생합니다. Hilt 테스트는 격리된 환경을 위해 **HiltTestApplication**이라는 전용 테스트 애플리케이션을 사용하도록 설정해야 합니다.
 *
 * 오류 메시지가 제안하는 대로, 테스트 환경이 HiltTestApplication을 사용하도록 테스트 러너(Test Runner)를 설정하면 문제를 해결할 수 있습니다.
 */
// A custom runner to set up the instrumented application class for tests.
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}