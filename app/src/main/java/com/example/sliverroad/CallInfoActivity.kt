// CallInfoActivity.kt
package com.example.sliverroad

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html.ImageGetter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// CallRequest 데이터 클래스에 필요한 모든 필드를 추가해주세요.
data class CallRequest(
    val id: Int,
    val fare: Int,
    val pickup: String,
    val dropoff: String,
    // 아래는 상세 정보용 필드
    val itemType: String,
    val itemName: String,
    val itemValue: Int,
    val itemWeight: Double,
    val clientName: String,
    val clientPhone: String,
    val pickupdate: String,
    val pickupAddr: String,
    val receiverName: String,
    val receiverPhone: String,
    val dropoffAddr: String,
    val note: String?
)

class CallInfoActivity : AppCompatActivity() {

    private lateinit var tvFare:        TextView
    private lateinit var tvItemType:    TextView
    private lateinit var tvItemName:    TextView
    private lateinit var tvItemValue:   TextView
    private lateinit var tvItemWeight:  TextView
    private lateinit var tvClientName:  TextView
    private lateinit var tvClientPhone: TextView
    private lateinit var tvPickupdate:  TextView
    private lateinit var tvPickupAddr:  TextView
    private lateinit var tvReceiverName:  TextView
    private lateinit var tvReceiverPhone: TextView
    private lateinit var tvDropoffAddr:   TextView
    private lateinit var tvNote:        TextView
    private lateinit var btnStartNavigation: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_info)

        // 1) 뷰 바인딩
        tvFare = findViewById(R.id.tvInfoFare)
        tvItemType = findViewById(R.id.tvItemType)
        tvItemName = findViewById(R.id.tvItemName)
        tvItemValue = findViewById(R.id.tvItemValue)
        tvItemWeight = findViewById(R.id.tvItemWeight)
        tvClientName = findViewById(R.id.tvClientName)
        tvClientPhone = findViewById(R.id.tvClientPhone)
        tvPickupdate = findViewById(R.id.tvPickupdate)
        tvPickupAddr = findViewById(R.id.tvPickupAddr)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone)
        tvDropoffAddr = findViewById(R.id.tvDropoffAddr)
        tvNote = findViewById(R.id.tvNote)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)

        // 2) 목업 데이터 생성
        val mockRequest = CallRequest(
            id = 123,
            fare = 23000,
            pickup = "강남구 역삼동",
            dropoff = "동대문구 회기동",
            itemType = "소형 가전",
            itemName = "전기포트",
            itemValue = 45000,
            itemWeight = 1.2,
            clientName = "홍길동",
            clientPhone = "010-1234-5678",
            pickupdate = "2025-05-30",
            pickupAddr = "서울 강남구 역삼동 123-4",
            receiverName = "김아무개",
            receiverPhone = "010-8765-4321",
            dropoffAddr = "서울 동대문구 회기동 56-7",
            note = "깨질 수 있으니 조심히 부탁드립니다."
        )

        // 3) 화면에 뿌리기
        tvItemType.text = mockRequest.itemType
        tvItemName.text = mockRequest.itemName
        tvItemValue.text = "${mockRequest.itemValue}원"
        tvItemWeight.text = "${mockRequest.itemWeight}kg"
        tvClientName.text = mockRequest.clientName
        tvClientPhone.text = mockRequest.clientPhone
        tvPickupdate.text = mockRequest.pickupdate
        tvPickupAddr.text = mockRequest.pickupAddr
        tvReceiverName.text = mockRequest.receiverName
        tvReceiverPhone.text = mockRequest.receiverPhone
        tvDropoffAddr.text = mockRequest.dropoffAddr
        tvNote.text = mockRequest.note ?: ""
        tvFare.text = "배송료 ${mockRequest.fare}원"

        btnStartNavigation.setOnClickListener {
            val intent = Intent(this, OsmMapActivity::class.java)
            intent.putExtra("${mockRequest.clientName}", "${mockRequest.clientPhone}")
            startActivity(intent)
        }
    }}
