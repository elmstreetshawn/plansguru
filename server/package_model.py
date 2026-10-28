import os
import zipfile

TOKENIZER_FILE = os.path.join("server", "tokenizer.model")
OUTPUT_TASK_FILE = os.path.join("server", "gemma-4-E2B-it-web.task")

def build_task_bundle():
    if not os.path.exists(TOKENIZER_FILE):
        print(f"Error: {TOKENIZER_FILE} not found.")
        return False

    server_files = os.listdir("server")
    model_src = None
    for f in server_files:
        if f.endswith(".old") or f.endswith(".bin") or f.endswith(".litertlm") or "gemma" in f.lower():
            if f != "gemma-4-E2B-it-web.task" and f != "tokenizer.model":
                model_src = os.path.join("server", f)
                break

    if not model_src:
        print("No raw model file found in server/ directory.")
        return False

    print(f"Packaging '{model_src}' as 'tf_lite_prefill_decode.bin' + '{TOKENIZER_FILE}' as 'tokenizer.model' into '{OUTPUT_TASK_FILE}'...")

    # MediaPipe litert_executor_utils.cc requires the model file inside the ZIP to be named 'tf_lite_prefill_decode.bin'
    with zipfile.ZipFile(OUTPUT_TASK_FILE, 'w', compression=zipfile.ZIP_STORED) as zipf:
        zipf.write(model_src, arcname="tf_lite_prefill_decode.bin")
        zipf.write(TOKENIZER_FILE, arcname="tokenizer.model")

    print(f"SUCCESS! Created MediaPipe Task Bundle: '{OUTPUT_TASK_FILE}' ({os.path.getsize(OUTPUT_TASK_FILE) / (1024*1024):.2f} MB)")
    return True

if __name__ == "__main__":
    build_task_bundle()
