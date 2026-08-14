// 프로필 사진처럼 브라우저에만 들고 있는 이미지를 다루는 도우미.
//
// 서버에 이미지 저장소가 아직 없어 사진은 base64로 localStorage에 남는다.
// 원본을 그대로 넣으면 폰 사진 한 장(3MB)이 base64로 4MB가 되고,
// localStorage 한도(브라우저 대개 5MB)에 걸려 저장이 통째로 실패한다.
// 그래서 화면에 필요한 크기로 줄인 뒤에 담는다.

// 프로필 아바타는 화면에서 100px 남짓으로 그려진다. 고해상도 화면을 감안해도 256px이면 충분하다.
export const MAX_PROFILE_IMAGE_SIZE = 256;

/**
 * 선택한 이미지 파일을 지정한 한 변 이하로 줄여 data URL로 돌려준다.
 * 원본이 이미 작으면 늘리지 않는다.
 */
export const readImageAsResizedDataUrl = (
  file,
  maxSize = MAX_PROFILE_IMAGE_SIZE,
) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onerror = () => reject(new Error('이미지를 읽지 못했습니다.'));

    reader.onload = () => {
      const image = new Image();

      image.onerror = () => reject(new Error('이미지를 열지 못했습니다.'));

      image.onload = () => {
        // 긴 변을 기준으로 줄여야 가로/세로 어느 쪽이든 한도 안에 들어온다.
        const scale = Math.min(
          1,
          maxSize / Math.max(image.width, image.height),
        );

        const width = Math.round(image.width * scale);
        const height = Math.round(image.height * scale);

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        canvas.getContext('2d').drawImage(image, 0, 0, width, height);

        // 원본 형식을 그대로 두면 PNG로 찍힌 사진이 줄여도 다시 커진다. JPEG로 고정한다.
        resolve(canvas.toDataURL('image/jpeg', 0.8));
      };

      image.src = reader.result;
    };

    reader.readAsDataURL(file);
  });
