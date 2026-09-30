package com.comentariobiblico.app.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.comentariobiblico.app.domain.model.CommentBlock
import com.comentariobiblico.app.domain.model.VerseId
import com.comentariobiblico.app.ui.theme.LocalReaderTypography
import com.comentariobiblico.app.ui.theme.searchHighlight

/**
 * Un bloque de comentario: el lema en negrita y su explicación.
 *
 * Cada bloque es un elemento independiente de la LazyColumn. Es obligatorio:
 * hay comentarios de 37.576 caracteres y un único Text con todo dentro
 * compondría y mediría el párrafo entero de golpe en cada recomposición.
 */
@Composable
fun CommentBlockText(
    block: CommentBlock,
    context: VerseId,
    onReference: (VerseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = LocalReaderTypography.current
    val colors = MaterialTheme.colorScheme

    val annotated = remember(block, colors) {
        buildAnnotatedString {
            block.lemma?.let { lemma ->
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = colors.secondary,
                    ),
                ) { append(lemma) }
                append(' ')
            }

            val start = length
            append(block.text)

            for (range in block.highlights) {
                addStyle(
                    SpanStyle(background = colors.searchHighlight),
                    start + range.first,
                    start + range.last + 1,
                )
            }

            for (link in findScriptureLinks(block.text, context)) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = link.target.raw.toString(),
                        styles = TextLinkStyles(
                            SpanStyle(
                                color = colors.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                        linkInteractionListener = { onReference(link.target) },
                    ),
                    start + link.range.first,
                    start + link.range.last + 1,
                )
            }
        }
    }

    Text(
        text = annotated,
        style = typography.body,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

/**
 * El texto bíblico.
 *
 * Las mayúsculas del inicio de capítulo y del Tetragrámaton ("EN EL principio",
 * "JEHOVA") son versalitas de la Reina-Valera, no énfasis: se renderizan con la
 * característica tipográfica `smcp` para que no parezcan un grito.
 */
@Composable
fun VerseText(text: String, modifier: Modifier = Modifier) {
    val typography = LocalReaderTypography.current

    val annotated = remember(text) {
        val match = SMALL_CAPS.find(text)
        if (match == null) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(fontFeatureSettings = SMALL_CAPS_FEATURE)) {
                    append(match.value)
                }
                append(text.substring(match.value.length))
            }
        }
    }

    Column(modifier) {
        Text(annotated, style = typography.verse)
    }
}

private val SMALL_CAPS = Regex("""^\p{Lu}{2,}(?:\s+\p{Lu}{2,})*""")
private const val SMALL_CAPS_FEATURE = "smcp"
