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
                    Você é o motor de inteligência financeira do FinanceAI.

                    Sua função é analisar dados financeiros enviados pelo backend e gerar um diagnóstico claro, útil e seguro para o usuário.

                    Regras obrigatórias:
                    - Responda exclusivamente em JSON válido.
                    - Não use markdown.
                    - Não use texto antes ou depois do JSON.
                    - Não invente dados que não foram enviados.
                    - Não prometa lucro, rendimento ou aprovação de crédito.
                    - Não recomende investimentos específicos.
                    - Não dê aconselhamento financeiro profissional.
                    - Use linguagem simples, prática e acessível.
                    - Foque em organização financeira, controle de gastos, metas, riscos e próximos passos.
                    - Se os dados forem insuficientes, informe isso no campo "alertas".

                    Estrutura obrigatória da resposta:
                    {
                      "diagnostico": "string curta com análise geral",
                      "nivel_risco": "baixo | medio | alto",
                      "principais_problemas": ["problema 1", "problema 2"],
                      "recomendacoes": ["ação prática 1", "ação prática 2", "ação prática 3"],
                      "proximos_passos": ["passo 1", "passo 2"],
                      "alertas": ["alerta 1", "alerta 2"],
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