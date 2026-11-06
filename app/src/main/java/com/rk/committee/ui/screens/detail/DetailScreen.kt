package com.rk.committee.ui.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rk.committee.R
import com.rk.committee.data.local.MemberEntity
import com.rk.committee.report.ReportGenerator
import com.rk.committee.ui.components.ConfirmDialog
import com.rk.committee.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** null id = add new member */
private data class MemberDialogState(val id: Long?, val name: String = "", val phone: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val details by vm.details.collectAsStateWithLifecycle()
    val selectedRound by vm.selectedRound.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDeleteCommittee by remember { mutableStateOf(false) }
    var memberDialog by remember { mutableStateOf<MemberDialogState?>(null) }
    var confirmDeleteMember by remember { mutableStateOf<MemberEntity?>(null) }
    var confirmRemovePayout by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(resources.getString(it)) }
    }

    val d = details
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(d?.committee?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (d != null) {
                        IconButton(onClick = {
                            if (d.members.isEmpty()) {
                                scope.launch { snackbar.showSnackbar(resources.getString(R.string.msg_add_members_first)) }
                            } else {
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) { ReportGenerator.create(context, d) }
                                    ReportGenerator.share(context, file)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.PictureAsPdf, stringResource(R.string.share_report))
                        }
                        IconButton(onClick = { onEdit(d.committee.id) }) {
                            Icon(Icons.Filled.Edit, stringResource(R.string.edit_committee))
                        }
                        IconButton(onClick = { confirmDeleteCommittee = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete_committee))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (d != null && tab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { memberDialog = MemberDialogState(id = null) },
                    icon = { Icon(Icons.Filled.PersonAdd, null) },
                    text = { Text(stringResource(R.string.add_member)) },
                )
            }
        },
    ) { padding ->
        if (d == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_overview)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_members)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.tab_payments)) })
            }
            when (tab) {
                0 -> OverviewTab(
                    d = d,
                    onOpenPayments = { round -> vm.selectRound(round); tab = 2 },
                    onAddMembers = { tab = 1 },
                )
                1 -> MembersTab(
                    d = d,
                    onEdit = { m -> memberDialog = MemberDialogState(m.id, m.name, m.phone) },
                    onMove = vm::moveMember,
                )
                else -> PaymentsTab(
                    d = d,
                    round = (selectedRound ?: d.currentRound).coerceIn(0, (d.totalRounds - 1).coerceAtLeast(0)),
                    onSelectRound = vm::selectRound,
                    onTogglePaid = vm::togglePaid,
                    onMarkAllPaid = vm::markAllPaid,
                    onGivePayout = { vm.setPayout(it, true) },
                    onRemovePayout = { confirmRemovePayout = it },
                    onAddMembers = { tab = 1 },
                )
            }
        }
    }

    memberDialog?.let { state ->
        MemberDialog(
            initial = state,
            onDismiss = { memberDialog = null },
            onSave = { name, phone ->
                vm.saveMember(state.id, name, phone)
                memberDialog = null
            },
            onDelete = state.id?.let { id ->
                {
                    confirmDeleteMember = d?.members?.firstOrNull { it.id == id }
                    memberDialog = null
                }
            },
        )
    }

    confirmDeleteMember?.let { m ->
        ConfirmDialog(
            title = stringResource(R.string.delete_member),
            message = stringResource(R.string.delete_member_message, m.name),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { vm.deleteMember(m.id) },
            onDismiss = { confirmDeleteMember = null },
        )
    }

    confirmRemovePayout?.let { round ->
        ConfirmDialog(
            title = stringResource(R.string.undo_payout),
            message = stringResource(R.string.undo_payout_message),
            confirmText = stringResource(R.string.undo),
            destructive = true,
            onConfirm = { vm.setPayout(round, false) },
            onDismiss = { confirmRemovePayout = null },
        )
    }

    if (confirmDeleteCommittee) {
        ConfirmDialog(
            title = stringResource(R.string.delete_committee),
            message = stringResource(R.string.delete_committee_message),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { vm.deleteCommittee(onBack) },
            onDismiss = { confirmDeleteCommittee = false },
        )
    }
}

@Composable
private fun MemberDialog(
    initial: MemberDialogState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var phone by rememberSaveable { mutableStateOf(initial.phone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == null) R.string.add_member else R.string.edit_member)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text(stringResource(R.string.member_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { v -> phone = v.filter { it.isDigit() || it == '+' }.take(15) },
                    label = { Text(stringResource(R.string.phone_optional)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
