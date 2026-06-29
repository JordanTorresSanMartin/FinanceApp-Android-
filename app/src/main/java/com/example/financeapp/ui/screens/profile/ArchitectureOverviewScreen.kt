package com.example.financeapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.financeapp.ui.theme.FinanceAppTheme

@Preview(showBackground = true)
@Composable
fun ArchitectureOverviewPreview() {
    FinanceAppTheme {
        ArchitectureOverviewScreen(onBack = {})
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitectureOverviewScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arquitectura Técnica", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { HeaderSection() }
            item { MacroCategoriesSection() }
            item { ArchitectureLayersSection() }
            item { FlowDiagramSection() }
            item { PortfolioSection() }
            item { DatabaseSchemaSection() }
            item { AndroidUiSystemSection() }
            item { HapticsSection() }
            item { IntegrationsSection() }
            item { BuildVersionSection() }
        }
    }
}

@Composable
fun HeaderSection() {
    Column {
        Text(
            "FinanceApp - Arquitectura técnica",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Planificación y desarrollo de una aplicación móvil con Kotlin/Compose, Supabase y PostgreSQL. Gestión de presupuesto y sincronización bancaria vía Fintoc/Plaid.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("8", "Perfiles")
            StatItem("5", "Módulos core")
            StatItem("Android", "Plataforma nativa")
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun MacroCategoriesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Macro-categorías")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryTag("Backend & Services", MaterialTheme.colorScheme.primaryContainer)
            CategoryTag("Database", MaterialTheme.colorScheme.secondaryContainer)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryTag("Mobile App (Kotlin/Compose)", MaterialTheme.colorScheme.tertiaryContainer)
            CategoryTag("API & Open Banking", Color(0xFFE8F5E9))
        }
    }
}

@Composable
fun CategoryTag(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ArchitectureLayersSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Capas de arquitectura")
        LayerItem("Presentation Layer", "UI / ViewModels / Navigation / Theme", Color(0xFFBBDEFB))
        LayerItem("Domain Layer (Business Logic)", "Use Cases / Repository Interfaces / Models", Color(0xFFC8E6C9))
        LayerItem("Data Layer", "Supabase Client / Postgrest API / Local Persistence", Color(0xFFFFCCBC))
    }
}

@Composable
fun LayerItem(title: String, description: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = color)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FlowDiagramSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Flujo de autenticación y navegación")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Supabase Auth -> Session -> MainGraph / AuthGraph",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PortfolioSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Módulos y Funcionalidades")
        ModuleItem("AU", "Autenticación", "Gestión de sesiones vía Supabase Auth (Email/Google).", "Active")
        ModuleItem("DB", "Dashboard", "Resumen mensual de balance, ingresos y gastos.", "Stable")
        ModuleItem("TR", "Transacciones", "Historial detallado y creación de nuevos registros.", "Core")
        ModuleItem("BG", "Presupuestos", "Configuración y seguimiento de límites por categoría.", "Core")
    }
}

@Composable
fun ModuleItem(code: String, name: String, desc: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DatabaseSchemaSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Esquema de base de datos (PostgreSQL)")
        SchemaTable()
    }
}

@Composable
fun SchemaTable() {
    Column(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            TableCell("Tabla", weight = 1f)
            TableCell("Descripción", weight = 2f)
        }
        HorizontalDivider()
        SchemaRow("transactions", "Registros de ingresos y gastos.")
        SchemaRow("categories", "Maestro de categorías (Icono, Color).")
        SchemaRow("budgets", "Presupuestos mensuales por categoría.")
        SchemaRow("monthly_summary", "Vista calculada para el Dashboard.")
    }
}

@Composable
fun SchemaRow(table: String, desc: String) {
    Row {
        TableCell(table, weight = 1f)
        TableCell(desc, weight = 2f)
    }
    HorizontalDivider()
}

@Composable
fun RowScope.TableCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(8.dp),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun AndroidUiSystemSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Sistema de diseño (Android)")
        Text("Material 3 Design System", fontWeight = FontWeight.Bold)
        Text("• Color Palette: Dynamic / Custom Green-Red", style = MaterialTheme.typography.bodySmall)
        Text("• Typography: Roboto / Inter", style = MaterialTheme.typography.bodySmall)
        Text("• Navigation: Type-safe Navigation Compose", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HapticsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Haptic Feedback")
        Text("Uso de LocalHapticFeedback para mejorar la experiencia de usuario en acciones críticas como navegación y creación de transacciones.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun IntegrationsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Integraciones Externas")
        IntegrationItem("Supabase", "Backend as a Service (Auth, DB, Storage).")
        IntegrationItem("Fintoc", "API de Open Banking para sincronización de cuentas.")
    }
}

@Composable
fun IntegrationItem(name: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BuildVersionSection() {
    Text(
        "Build Version: 1.0.0-stable\nKotlin 2.0.0 | Compose 1.7.0",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
