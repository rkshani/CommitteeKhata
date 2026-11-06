package com.rk.committee.ui.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rk.committee.R
import com.rk.committee.data.local.CommitteeEntity
import com.rk.committee.data.repository.CommitteeRepository
import com.rk.committee.ui.components.LoadingBox
import com.rk.committee.ui.components.SectionCard
import com.rk.committee.ui.util.monthLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class EditCommitteeState(
    val loading: Boolean = false,
    val isEdit: Boolean = false,
    val name: String = "",
    val amount: String = "",
    val startYear: Int,
    val startMonth: Int,
    val notes: String = "",
    val nameError: Boolean = false,
    val amountError: Boolean = false,
    val savedId: Long? = null,
) {
    val start: YearMonth get() = YearMonth.of(startYear, startMonth)
}

@HiltViewModel
class EditCommitteeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CommitteeRepository,
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("id") ?: -1L
    private var original: CommitteeEntity? = null

    private val _state = MutableStateFlow(
        YearMonth.now().let { EditCommitteeState(loading = id > 0, isEdit = id > 0, startYear = it.year, startMonth = it.monthValue) },
    )
    val state = _state.asStateFlow()

    init {
        if (id > 0) {
            viewModelScope.launch {
                val c = repo.getCommittee(id)
                original = c
                _state.update {
                    if (c == null) it.copy(loading = false)
                    else it.copy(
                        loading = false,
                        name = c.name,
                        amount = c.amountPerMember.toString(),
                        startYear = c.startYear,
                        startMonth = c.startMonth,
                        notes = c.notes,
                    )
                }
            }
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v.take(60), nameError = false) }
    fun onAmount(v: String) = _state.update { it.copy(amount = v.filter(Char::isDigit).take(10), amountError = false) }
    fun onNotes(v: String) = _state.update { it.copy(notes = v.take(300)) }
    fun shiftMonth(delta: Long) = _state.update {
        val ym = it.start.plusMonths(delta)
        it.copy(startYear = ym.year, startMonth = ym.monthValue)
    }

    fun save() {
        val s = _state.value
        val amount = s.amount.toLongOrNull()
        val nameBad = s.name.isBlank()
        if (nameBad || amount == null || amount <= 0L) {
            _state.update { it.copy(nameError = nameBad, amountError = amount == null || amount <= 0L) }
            return
        }
        viewModelScope.launch {
            val base = original ?: CommitteeEntity(name = "", amountPerMember = 0, startYear = s.startYear, startMonth = s.startMonth)
            val savedId = repo.saveCommittee(
                base.copy(
                    name = s.name.trim(),
                    amountPerMember = amount,
                    startYear = s.startYear,
                    startMonth = s.startMonth,
                    notes = s.notes.trim(),
                ),
            )
            _state.update { it.copy(savedId = savedId) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommitteeScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    vm: EditCommitteeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.savedId) {
        s.savedId?.let { onSaved(it, !s.isEdit) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (s.isEdit) R.string.edit_committee else R.string.new_committee)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (s.loading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = s.name,
                onValueChange = vm::onName,
                label = { Text(stringResource(R.string.committee_name)) },
                placeholder = { Text(stringResource(R.string.committee_name_hint)) },
                isError = s.nameError,
                supportingText = if (s.nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.amount,
                onValueChange = vm::onAmount,
                label = { Text(stringResource(R.string.amount_per_member)) },
                prefix = { Text(stringResource(R.string.currency_prefix)) },
                isError = s.amountError,
                supportingText = {
                    Text(stringResource(if (s.amountError) R.string.error_amount_required else R.string.amount_help))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionCard(title = stringResource(R.string.start_month)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { vm.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
                    }
                    Text(
                        monthLabel(s.start),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = { vm.shiftMonth(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.start_month_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = s.notes,
                onValueChange = vm::onNotes,
                label = { Text(stringResource(R.string.notes_optional)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
