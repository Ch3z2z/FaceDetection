import cv2
import numpy as np
import tensorflow as tf

model = tf.keras.models.load_model("facial_keypoints_model.keras")

face_cascade = cv2.CascadeClassifier(
    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
)


cap = cv2.VideoCapture(0)

if not cap.isOpened():
    raise RuntimeError("Не удалось открыть веб-камеру")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    gray_full = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)


    faces = face_cascade.detectMultiScale(
        gray_full,
        scaleFactor=1.2,
        minNeighbors=5,
        minSize=(80, 80)
    )

    for (x, y, w, h) in faces:
        face = gray_full[y:y+h, x:x+w]

        if face.size == 0:
            continue

        face_resized = cv2.resize(face, (96, 96))
        face_norm = face_resized / 255.0
        input_img = face_norm.reshape(1, 96, 96, 1).astype(np.float32)

        preds = model.predict(input_img, verbose=0)[0]

        for i in range(0, len(preds), 2):
            px = int(preds[i] * w / 96) + x
            py = int(preds[i + 1] * h / 96) + y
            cv2.circle(frame, (px, py), 2, (0, 0, 255), -1)

        cv2.rectangle(frame, (x, y), (x+w, y+h), (255, 0, 0), 2)

    cv2.imshow("Facial Keypoints Detection", frame)

    if cv2.waitKey(1) & 0xFF == 27:
        break

cap.release()
cv2.destroyAllWindows()
