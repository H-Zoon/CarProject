import com.devidea.chevy.carsystem.CarModel

class ModelMsgHandler(private val models: CarModel) {

    companion object {
        const val KEY_MSG_APP = "KEY_MSG_APP"
        const val KEY_MSG_DEVICE = "KEY_MSG_DEVICE"
        const val KEY_NAVI_INFO = "KEY_NAVI_INFO"
        const val KEY_VIEW_EVENT = "KEY_VIEW_EVENT"
        const val MSG_FROM_APP = 1
        const val MSG_FROM_DEVICE = 5
        const val MSG_VIEW_EVENT = 2
        const val tag = "model"
    }

    fun handleMessage(message: ByteArray) {
        //val byteArray = message.data.getByteArray(KEY_MSG_DEVICE)
        //message.let { handMsgFromDevice(it, it.size) }

        handMsgFromDevice(message, message.size)

        //Log.d(tag, "msg:${message.what}")
        /*when (message.what) {
            //MSG_FROM_APP -> handleAppMsg(message.data.getByteArray(KEY_MSG_APP))
            //MSG_VIEW_EVENT -> AppModel.getInstance().onClkView(message.arg1)
            MSG_FROM_DEVICE -> {
                val byteArray = message.data.getByteArray(KEY_MSG_DEVICE)
                byteArray?.let { handMsgFromDevice(it, it.size) }
            }
        }*/
    }

    private fun handMsgFromDevice(bArr: ByteArray, MesseageSize: Int) {
        if (MesseageSize == 0 || bArr.isEmpty()) return

        val bArr2 = ByteArray(MesseageSize)
        System.arraycopy(bArr, 0, bArr2, 0, MesseageSize)
        val b = bArr2[0].toInt()

        when (b) {
            1 -> models.devModule.onRecvMsg(bArr2, bArr2.size)
            //8 -> AppModel.getInstance().onRecvMsg(bArr2, bArr2.size)
            63 -> {
                if (bArr2.size > 2 && bArr2[1] == 16.toByte()) {
                    val cArr = CharArray(bArr2.size - 2) { bArr2[it + 2].toChar() }
                    //AppModel.getInstance().setAgencyCode(String(cArr))
                }
            }

            3 -> models.devModule.onRecvMsg(bArr2, bArr2.size) // Do nothing
            4 -> models.tpmsModule.onRecvTPMS(bArr2)
            5, 6, 55 -> models.controlModule.onRecvMsg(bArr2, bArr2.size)
            /*52 -> {
                ImageSender.getInstance().onRecvMessage(bArr2, i)
                if (bArr2[1] == 49.toByte()) {
                    NaviModel.getInstance().onRequestTrafficData()
                }
            }*/
            //53 -> PhoneModel.getIns(SinjetApplication.getInstance()).onRecvMsg(bArr2, bArr2.size)
            //54 -> DynamicFontModel.getInstance().onRecvDynamicFontMsg(bArr2)
            /*55, 56, 57 -> {
                CarModel.getInstance().devModule.setMCUUpgradeMethod(1)
                McuUpgradeModel.getInstance().onRecvUpgradeMsg(bArr2, i)
            }
            58 -> {
                CarModel.getInstance().devModule.setMCUUpgradeMethod(2)
                McuUpgradeModelSimple.getInstance().onRecvUpgradeMsg(bArr2, i)
                if (i >= 14 && bArr2[1] == 17.toByte()) {
                    BluetoothModel.getInstance().onTransportSpeed(
                        Data.byte2int(bArr2[10], bArr2[11], bArr2[12], bArr2[13])
                    )
                }
            }*/
        }
    }

    private fun handleAppMsg(bArr: ByteArray?) {
        bArr?.let {
            if (it[0] == 1.toByte()) {
                //AppModel.getInstance().onEnterPage(it[1])
            }
        }
    }
}
