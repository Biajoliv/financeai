package com.financeai.infrastructure.ai;

import com.financeai.domain.port.InsightGenerator;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("openai")
public class OpenAiInsightGenerator implements InsightGenerator {

    private final OpenAIClient client;

    public OpenAiInsightGenerator() {
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public String gerar(String prompt) {
        try {
            String systemPrompt = """
                    Você é uma IA financeira do projeto FinanceAI.

                    Analise o objetivo financeiro do usuário de forma prática, segura e objetiva.

                    Regras:
                    - Responda apenas em JSON válido.
                    - Não use markdown.
                    - Não invente dados bancários.
                    - Não prometa rendimento financeiro.
                    - Não recomende investimentos de alto risco.
                    - Foque em organização financeira, redução de gastos, metas e educação financeira.
                    - A resposta deve ser clara para usuários leigos.

                    Formato obrigatório:
                    {
                      "diagnostico": "...",
                      "recomendacoes": ["...", "..."],
                      "riscos": ["...", "..."],
                      "proximos_passos": ["...", "..."],
                      "confianca": 0.0
                    }
                    """;

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4_1_MINI)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(prompt)
                    .build();

            ChatCompletion response = client.chat().completions().create(params);

            return response.choices()
                    .get(0)
                    .message()
                    .content()
                    .orElse("{\"erro\":\"A IA não retornou conteúdo.\"}");

        } catch (Exception e) {
            return """
                    {
                      "erro": "IA indisponível. Verifique a chave da OpenAI, o saldo da conta ou a configuração do ambiente."
                    }
                    """;
        }
    }
}