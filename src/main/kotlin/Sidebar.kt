import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Sidebar(selected: Int, onSelect: (select: Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(160.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "RC Test Stand", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
//        SidebarItem(0, "How to Start", selected, onSelect = { onSelect(it) })
        SidebarItem(0, "Plots", selected, onSelect = { onSelect(it) })
        Spacer(modifier = Modifier.weight(1.0F))
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "©RC Test Stand. All rights are reserved.", fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun SidebarItem(index: Int, text: String, selected: Int, onSelect: (select: Int) -> Unit) {
    val isActive = index == selected
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(0.dp),
        backgroundColor = if (isActive) Color.DarkGray else Color.LightGray,
        onClick = { onSelect(index) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        }
    }
}